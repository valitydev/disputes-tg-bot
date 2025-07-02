package dev.vality.disputes.tg.bot.merchant.handler.command;

import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.disputes.merchant.DisputeParams;
import dev.vality.disputes.tg.bot.core.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.core.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.core.service.external.HellgateService;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.merchant.converter.UpdateToAttachmentConverter;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.merchant.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.merchant.exception.AttachmentTypeNotSupportedException;
import dev.vality.disputes.tg.bot.merchant.exception.PaymentCapturedException;
import dev.vality.disputes.tg.bot.merchant.exception.PaymentNotStartedException;
import dev.vality.disputes.tg.bot.merchant.exception.PaymentStatusRestrictionException;
import dev.vality.disputes.tg.bot.merchant.handler.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.merchant.service.external.DisputesApiService;
import dev.vality.disputes.tg.bot.merchant.util.PolyglotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDisputeHandler implements MerchantMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/c ";
    private static final String CYRILLIC_SINGLE_LETTER = "/с ";
    private static final String FULL_COMMAND = "/check ";

    private final MerchantDisputeDao merchantDisputeDao;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;
    private final HellgateService hellgateService;
    private final UpdateToAttachmentConverter attachmentConverter;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(MerchantMessageDto message) {
        var update = message.getUpdate();
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @Transactional
    public void handle(MerchantMessageDto message) throws TelegramApiException {
        log.info("[{}] Processing create dispute request", message.getUpdate().getUpdateId());
        Update update = message.getUpdate();
        String messageText = extractText(update);
        Optional<DisputeInfoDto> paymentInfoOptional = TextParsingUtil.getDisputeInfo(messageText);
        Locale replyLocale = polyglot.getLocale(message.getMerchantChat().getLocale());
        if (paymentInfoOptional.isEmpty()) {
            log.warn("[{}] Payment info not found, message text: {}", update.getUpdateId(), messageText);
            String reply = polyglot.getText(replyLocale, "error.input.invoice-missing");
            telegramClient.execute(buildPlainTextResponse(update, reply));
            return;
        }
        telegramClient.execute(handle(message, telegramClient, paymentInfoOptional.get(), replyLocale));
    }

    @Transactional
    public SendMessage handle(MerchantMessageDto message, TelegramClient telegramClient,
                              DisputeInfoDto disputeInfo, Locale replyLocale) {
        var update = message.getUpdate();
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
            String reply = PolyglotUtil.prepareStatusMessage(dispute, replyLocale, polyglot);
            return buildPlainTextResponse(update, reply);
        }

        DisputeParams disputeParams = new DisputeParams();
        disputeParams.setInvoiceId(disputeInfo.getInvoiceId());
        disputeParams.setPaymentId(disputeInfo.getPaymentId());

        // Download and parse attachments
        try {
            var attachment = attachmentConverter.convert(update, telegramClient);
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
                MerchantDispute dispute = buildDispute(message, disputeInfo);
                merchantDisputeDao.save(dispute);
                String reply = buildDisputeCreatedReply(dispute, replyLocale);
                return buildPlainTextResponse(update, reply);
            } else {
                log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
            }
        } catch (PaymentNotStartedException e) {
            log.error("[{}] PaymentNotStartedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-not-started");
            return buildPlainTextResponse(update, reply);
        } catch (PaymentCapturedException e1) {
            log.error("[{}] PaymentCapturedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-captured");
            return buildPlainTextResponse(update, reply);
        } catch (PaymentStatusRestrictionException e2) {
            log.error("[{}] PaymentStatusRestrictionException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-status-restriction");
            return buildPlainTextResponse(update, reply);
        } catch (AttachmentTypeNotSupportedException e3) {
            log.error("[{}] AttachmentTypeNotSupportedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.attachment-type-not-supported");
            return buildPlainTextResponse(update, reply);
        } catch (Exception unknownException) {
            log.error("[{}] Unexpected error occurred", update.getUpdateId(), unknownException);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            return buildPlainTextResponse(update, reply);
        }
        log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
        String reply = polyglot.getText(replyLocale, "error.unknown");
        return buildPlainTextResponse(update, reply);
    }

    private String buildDisputeCreatedReply(MerchantDispute dispute, Locale replyLocale) {
        return dispute.getExternalId() == null
                ? polyglot.getText(replyLocale, "dispute.created-wo-external-id",
                        dispute.getInvoiceId()) :
                polyglot.getText(replyLocale, "dispute.created",
                        dispute.getInvoiceId(),
                        dispute.getExternalId());
    }

    private void fillMissingPaymentInfo(DisputeInfoDto disputeInfoDto) throws InvoiceNotFound {
        Invoice invoice = hellgateService.getInvoice(disputeInfoDto.getInvoiceId());
        if (disputeInfoDto.getPaymentId() == null) {
            disputeInfoDto.setPaymentId(invoice.getPayments().getLast().getPayment().getId());
        }
        disputeInfoDto.setExternalId(invoice.getInvoice().getExternalId());
    }

    private MerchantDispute buildDispute(MerchantMessageDto message, DisputeInfoDto paymentInfo) {
        MerchantDispute dispute = new MerchantDispute();
        dispute.setId(UUID.fromString(paymentInfo.getDisputeId()));
        dispute.setCreatedAt(LocalDateTime.now());
        dispute.setUpdatedAt(LocalDateTime.now());
        dispute.setChatId(message.getMerchantChat().getId());
        dispute.setInvoiceId(paymentInfo.getInvoiceId());
        dispute.setPaymentId(paymentInfo.getPaymentId());
        dispute.setExternalId(paymentInfo.getExternalId());
        dispute.setTgMessageId(Long.valueOf(TelegramUtil.getMessage(message.getUpdate()).getMessageId()));
        dispute.setStatus(DisputeStatus.pending);
        return dispute;
    }
}
