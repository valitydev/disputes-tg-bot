package dev.vality.disputes.tg.bot.handler.merchant.command;

import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.disputes.merchant.DisputeParams;
import dev.vality.disputes.tg.bot.converter.UpdateToAttachmentConverter;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.exception.AttachmentTypeNotSupportedException;
import dev.vality.disputes.tg.bot.exception.PaymentCapturedException;
import dev.vality.disputes.tg.bot.exception.PaymentNotStartedException;
import dev.vality.disputes.tg.bot.exception.PaymentStatusRestrictionException;
import dev.vality.disputes.tg.bot.handler.merchant.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DisputesApiService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import dev.vality.disputes.tg.bot.util.PolyglotUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;
import static dev.vality.disputes.tg.bot.util.TelegramUtil.hasAttachment;

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
    private final TelegramApiService telegramApiService;

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
            telegramApiService.sendReplyTo(reply, update);
            return;
        }
        handle(message, paymentInfoOptional.get(), replyLocale);
    }

    @Transactional
    public void handle(MerchantMessageDto message,
                              DisputeInfoDto disputeInfo, Locale replyLocale) {
        var update = message.getUpdate();
        if (!hasAttachment(update)) {
            log.warn("[{}] Attachment missing", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.attachment-missing");
            telegramApiService.sendReplyTo(reply, update);
            return;
        }

        // Enrich payment context with data from hellgate
        try {
            fillMissingPaymentInfo(disputeInfo);
        } catch (InvoiceNotFound e) {
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            telegramApiService.sendReplyTo(reply, update);
            return;
        } catch (Exception e1) {
            String reply = polyglot.getText(replyLocale, "error.unknown");
            telegramApiService.sendReplyTo(reply, update);
            return;
        }

        // Check if dispute already exists and is not resolved
        var disputeOptional = merchantDisputeDao.getPendingDisputeByInvoiceIdAndPaymentId(disputeInfo);
        if (disputeOptional.isPresent()) {
            var dispute = disputeOptional.get();
            log.info("[{}] Dispute has been already created", update.getUpdateId());
            String reply = PolyglotUtil.prepareStatusMessage(dispute, replyLocale, polyglot);
            telegramApiService.sendReplyTo(reply, update);
            return;
        }

        DisputeParams disputeParams = new DisputeParams();
        disputeParams.setInvoiceId(disputeInfo.getInvoiceId());
        disputeParams.setPaymentId(disputeInfo.getPaymentId());

        // Download and parse attachments
        try {
            var attachment = attachmentConverter.convert(update);
            disputeParams.setAttachments(List.of(attachment));
        } catch (TelegramApiException | IOException e) {
            log.error("[{}] Unable to process attached file", update.getUpdateId(), e);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            telegramApiService.sendReplyTo(reply, update);
            return;
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
                telegramApiService.sendReplyTo(reply, update);
                return;
            } else {
                log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
            }
        } catch (PaymentNotStartedException e) {
            log.error("[{}] PaymentNotStartedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-not-started");
            telegramApiService.sendReplyTo(reply, update);
            return;
        } catch (PaymentCapturedException e1) {
            log.error("[{}] PaymentCapturedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-captured");
            telegramApiService.sendReplyTo(reply, update);
            return;
        } catch (PaymentStatusRestrictionException e2) {
            log.error("[{}] PaymentStatusRestrictionException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.payment-status-restriction");
            telegramApiService.sendReplyTo(reply, update);
            return;
        } catch (AttachmentTypeNotSupportedException e3) {
            log.error("[{}] AttachmentTypeNotSupportedException occurred", update.getUpdateId());
            String reply = polyglot.getText(replyLocale, "error.input.attachment-type-not-supported");
            telegramApiService.sendReplyTo(reply, update);
            return;
        } catch (Exception unknownException) {
            log.error("[{}] Unexpected error occurred", update.getUpdateId(), unknownException);
            String reply = polyglot.getText(replyLocale, "error.unknown");
            telegramApiService.sendReplyTo(reply, update);
            return;
        }
        log.warn("[{}] This point should not be reached, check previous logs", update.getUpdateId());
        String reply = polyglot.getText(replyLocale, "error.unknown");
        telegramApiService.sendReplyTo(reply, update);
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
