package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.disputes.merchant.DisputeParams;
import dev.vality.disputes.tg.bot.common.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.merchant.converter.UpdateToAttachmentConverter;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.merchant.service.external.DisputesApiService;
import dev.vality.disputes.tg.bot.merchant.service.external.HellgateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDisputeHandler implements CommonHandler {

    private static final String LATIN_SINGLE_LETTER = "/c ";
    private static final String CYRILLIC_SINGLE_LETTER = "/с ";
    private static final String FULL_COMMAND = "/check ";

    private final MerchantDisputeDao merchantDisputeDao;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;
    private final HellgateService hellgateService;
    private final UpdateToAttachmentConverter attachmentConverter;

    @Override
    public boolean filter(MessageDto messageDto) {
        var update = messageDto.getUpdate();
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!ChatType.merchant.equals(messageDto.getChat().getType())) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @SneakyThrows
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        log.info("[{}] Processing create dispute request", messageDto.getUpdate().getUpdateId());
        Update update = messageDto.getUpdate();
        String messageText = extractText(update);
        Optional<DisputeInfoDto> paymentInfoOptional = TextParsingUtil.getDisputeInfo(messageText);
        String replyLocale = polyglot.getLocale(update, messageDto.getChat());
        if (paymentInfoOptional.isEmpty()) {
            log.warn("[{}] Payment info not found, message text: {}", update.getUpdateId(), messageText);
            String reply = polyglot.getText(replyLocale, "error.input.invoice-missing");
            disputesBot.execute(buildPlainTextResponse(update, reply));
            return;
        }
        disputesBot.execute(handle(update, disputesBot, paymentInfoOptional.get(), replyLocale));
    }

    public SendMessage handle(Update update, DisputesBot disputesBot,
                              DisputeInfoDto disputeInfo, String replyLocale) {
        if (!hasAttachment(update)) {
            log.warn("[{}] Attachment missing", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.attachment-missing");
            return buildPlainTextResponse(update, reply);
        }

        // Enrich payment context with data from hellgate
        try {
            fillMissingPaymentInfo(disputeInfo);
        } catch (InvoiceNotFound e) {
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            return buildPlainTextResponse(update, reply);
        } catch (Exception e1) {
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }

        // Check if dispute already exists and is not resolved
        var disputeOptional = merchantDisputeDao.getPendingDisputeByInvoiceIdAndPaymentId(disputeInfo);
        if (disputeOptional.isPresent()) {
            var dispute = disputeOptional.get();
            log.info("[{}] Dispute has been already created", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "dispute.status",
                    dispute.getDisputeId(),
                    dispute.getInvoiceId(),
                    dispute.getExternalId(),
                    dispute.getStatus().getLiteral(),
                    dispute.getUpdatedAt());
            return buildPlainTextResponse(update, reply);
        }

        DisputeParams disputeParams = new DisputeParams();
        disputeParams.setInvoiceId(disputeInfo.getInvoiceId());
        disputeParams.setPaymentId(disputeInfo.getPaymentId());

        // Download and parse attachments
        try {
            var attachment = attachmentConverter.convert(update, disputesBot);
            disputeParams.setAttachments(List.of(attachment));
        } catch (TelegramApiException | IOException e) {
            log.error("[{}] Unable to process attached file", update.getUpdateId(), e);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }

        // Create dispute via disputes-api
        try {
            var response = disputesApiService.createDispute(disputeParams);
            log.info("[{}] Dispute has been created: {}", update.getUpdateId(), response);
            if (response.isSetSuccessResult()) {
                disputeInfo.setDisputeId(response.getSuccessResult().getDisputeId());
                MerchantDispute dispute = buildDispute(update, disputeInfo);
                merchantDisputeDao.save(dispute);
                String reply = polyglot.getText(replyLocale, "dispute.created",
                        dispute.getDisputeId(),
                        dispute.getInvoiceId(),
                        dispute.getExternalId());
                return buildPlainTextResponse(update, reply);
            } else {
                log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
            }
        } catch (Exception e) {
            log.error("[{}] Unexpected error occurred", update.getUpdateId(), e);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }
        log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
        String reply = polyglot.getText(replyLocale, "error.unknown");
        return buildPlainTextResponse(update, reply);
    }

    private void fillMissingPaymentInfo(DisputeInfoDto disputeInfoDto) throws InvoiceNotFound {
        Invoice invoice = hellgateService.getInvoice(disputeInfoDto.getInvoiceId());
        if (disputeInfoDto.getPaymentId() == null) {
            disputeInfoDto.setPaymentId(invoice.getPayments().getLast().getPayment().getId());
        }
        disputeInfoDto.setExternalId(invoice.getInvoice().getExternalId());
    }

    private MerchantDispute buildDispute(Update update, DisputeInfoDto paymentInfo) {
        MerchantDispute dispute = new MerchantDispute();
        dispute.setCreatedAt(LocalDateTime.now());
        dispute.setUpdatedAt(LocalDateTime.now());
        dispute.setChatId(update.getMessage().getChatId());
        dispute.setInvoiceId(paymentInfo.getInvoiceId());
        dispute.setPaymentId(paymentInfo.getPaymentId());
        dispute.setDisputeId(paymentInfo.getDisputeId());
        dispute.setExternalId(paymentInfo.getExternalId());
        dispute.setTgMessageId(Long.valueOf(update.getMessage().getMessageId()));
        dispute.setTgMessageLocale(update.getMessage().getFrom().getLanguageCode());
        dispute.setStatus(DisputeStatus.pending);
        return dispute;
    }
}
