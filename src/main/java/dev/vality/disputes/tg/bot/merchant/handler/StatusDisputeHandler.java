package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.disputes.DisputeParams;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.dto.PaymentInfoDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.Dispute;
import dev.vality.disputes.tg.bot.merchant.converter.UpdateToAttachmentConverter;
import dev.vality.disputes.tg.bot.merchant.dao.DisputeDao;
import dev.vality.disputes.tg.bot.merchant.service.external.DisputesApiService;
import dev.vality.disputes.tg.bot.merchant.service.external.HellgateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDisputeHandler implements CommonHandler<SendMessage> {

    private static final String LATIN_SINGLE_LETTER = "/c ";
    private static final String CYRILLIC_SINGLE_LETTER = "/с ";
    private static final String FULL_COMMAND = "/check ";

    private final DisputeDao disputeDao;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;
    private final HellgateService hellgateService;
    private final UpdateToAttachmentConverter attachmentConverter;

    @Override
    public boolean filter(MessageDto messageDto) {
        var update = messageDto.getUpdate();
        if (!update.hasMessage() || extractText(update) == null) {
            return false;
        }

        if (!ChatType.merchant.equals(messageDto.getChat().getType())) {
            return false;
        }

        String messageText = extractText(update);
        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    public SendMessage handle(MessageDto messageDto, DisputesBot disputesBot) {
        Update update = messageDto.getUpdate();
        String messageText = extractText(update);
        String replyLocale = update.getMessage().getFrom().getLanguageCode();
        Optional<PaymentInfoDto> paymentInfoOptional = TextParsingUtil.getPaymentInfo(messageText);

        if (paymentInfoOptional.isEmpty()) {
            log.warn("Payment info not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.invoice-missing");
            return buildPlainTextResponse(update, reply);
        }

        if (!hasAttachment(update)) {
            log.warn("Attachment not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.attachment-missing");
            return buildPlainTextResponse(update, reply);
        }


        try {
            fillMissingPaymentInfo(paymentInfoOptional.get());
        } catch (InvoiceNotFound e) {
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            return buildPlainTextResponse(update, reply);
        } catch (Exception e1) {
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }

        PaymentInfoDto paymentInfo = paymentInfoOptional.get();
        var disputeOptional = disputeDao.getPendingDisputeByInvoiceIdAndPaymentId(paymentInfo);
        if (disputeOptional.isPresent()) {
            var dispute = disputeOptional.get();
            log.info("Dispute has been already created: {}", messageText);
            String reply = polyglot.getText(replyLocale, "dispute.status",
                    dispute.getDisputeId(),
                    dispute.getInvoiceId(),
                    dispute.getExternalId(),
                    dispute.getStatus().getLiteral());
            return buildPlainTextResponse(update, reply);
        }

        DisputeParams disputeParams = new DisputeParams();
        disputeParams.setInvoiceId(paymentInfo.getInvoiceId());
        disputeParams.setPaymentId(paymentInfo.getPaymentId());

        try {
            var attachment = attachmentConverter.convert(messageDto.getUpdate(), disputesBot);
            disputeParams.setAttachments(List.of(attachment));
        } catch (TelegramApiException | IOException e) {
            log.error("Unable to process attached file", e);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }


        try {
            var response = disputesApiService.createDispute(disputeParams);
            log.info("Dispute has been created: {}", response);
            if (response.isSetSuccessResult()) {
                paymentInfo.setDisputeId(response.getSuccessResult().getDisputeId());
                Dispute dispute = buildDispute(update, paymentInfo);
                disputeDao.save(dispute);
                String reply = polyglot.getText(replyLocale, "dispute.status",
                        dispute.getDisputeId(),
                        dispute.getInvoiceId(),
                        dispute.getExternalId());
                return buildPlainTextResponse(update, reply);
            } else {
                log.warn("This point should not be reached, check previous logs");
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }

        log.warn("This point should not be reached, check previous logs");
        String reply = polyglot.getText(replyLocale, "error.unknown");
        return buildPlainTextResponse(update, reply);
    }

    private SendMessage buildPlainTextResponse(Update update, String text) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .replyToMessageId(update.getMessage().getMessageId())
                .parseMode("MarkdownV2")
                .text(text)
                .build();
    }

    private boolean hasAttachment(Update update) {
        return update.getMessage().hasPhoto() || update.getMessage().hasDocument();
    }

    private void fillMissingPaymentInfo(PaymentInfoDto paymentInfoDto) throws InvoiceNotFound {
        Invoice invoice = hellgateService.getInvoice(paymentInfoDto.getInvoiceId());
        if (paymentInfoDto.getPaymentId() == null) {
            paymentInfoDto.setPaymentId(invoice.getPayments().getLast().getPayment().getId());
        }
        paymentInfoDto.setExternalId(invoice.getInvoice().getExternalId());
    }

    private Dispute buildDispute(Update update, PaymentInfoDto paymentInfo) {
        Dispute dispute = new Dispute();
        dispute.setCreatedAt(LocalDateTime.now());
        dispute.setUpdatedAt(LocalDateTime.now());
        dispute.setChatId(update.getMessage().getChatId());
        dispute.setInvoiceId(paymentInfo.getInvoiceId());
        dispute.setPaymentId(paymentInfo.getPaymentId());
        dispute.setDisputeId(paymentInfo.getDisputeId());
        dispute.setExternalId(paymentInfo.getExternalId());
        dispute.setMerchantTgMessageId(Long.valueOf(update.getMessage().getMessageId()));
        dispute.setStatus(DisputeStatus.pending);
        return dispute;
    }

    private String extractText(Update update) {
        return update.getMessage().hasText()
                ? update.getMessage().getText() : update.getMessage().getCaption();
    }
}
