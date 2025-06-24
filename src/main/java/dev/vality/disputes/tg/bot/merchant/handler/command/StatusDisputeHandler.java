package dev.vality.disputes.tg.bot.merchant.handler.command;

import dev.vality.disputes.merchant.DisputeContext;
import dev.vality.disputes.merchant.DisputeNotFound;
import dev.vality.disputes.merchant.DisputeStatusResult;
import dev.vality.disputes.tg.bot.core.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.core.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.core.exception.UnexpectedException;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.merchant.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.merchant.handler.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.merchant.service.external.DisputesApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;
import static dev.vality.disputes.tg.bot.merchant.util.PolyglotUtil.explanation;
import static dev.vality.disputes.tg.bot.merchant.util.PolyglotUtil.prepareStatusMessage;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("MissingSwitchDefault")
public class StatusDisputeHandler implements MerchantMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/s ";
    private static final String FULL_COMMAND = "/status ";

    private final MerchantDisputeDao merchantDisputeDao;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(MerchantMessageDto message) {
        String messageText = extractText(message.getUpdate());
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    public void handle(MerchantMessageDto message) throws TelegramApiException {
        Update update = message.getUpdate();
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale(message.getMerchantChat().getLocale());
        Optional<DisputeInfoDto> paymentInfoOptional = TextParsingUtil.getDisputeInfo(messageText);

        if (paymentInfoOptional.isEmpty()) {
            log.warn("Payment info not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.dispute-or-invoice-missing");
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }

        List<MerchantDispute> matchingDisputes = findDisputes(paymentInfoOptional.get());
        if (matchingDisputes.isEmpty()) {
            log.info("Dispute not found: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.dispute-not-found");
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }
        updateDisputesStatuses(matchingDisputes);
        String response = buildPlainTextResponse(matchingDisputes, replyLocale);
        telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, response));
    }

    private List<MerchantDispute> findDisputes(DisputeInfoDto paymentInfo) {
        if (paymentInfo.getDisputeId() != null) {
            Optional<MerchantDispute> optionalDispute = merchantDisputeDao.getByDisputeId(paymentInfo.getDisputeId());
            if (optionalDispute.isPresent()) {
                return List.of(optionalDispute.get());
            }
        }

        if (paymentInfo.getInvoiceId() != null) {
            return merchantDisputeDao.getByInvoiceId(paymentInfo.getInvoiceId());
        }
        return List.of();
    }

    public void updateDisputesStatuses(List<MerchantDispute> disputes) {
        for (MerchantDispute dispute : disputes) {
            DisputeContext disputeContext = new DisputeContext();
            disputeContext.setDisputeId(dispute.getId().toString());
            DisputeStatusResult result;
            try {
                result = disputesApiService.getStatus(disputeContext);
                updateDisputeInfo(dispute, result);
                merchantDisputeDao.update(dispute);
            } catch (DisputeNotFound e) {
                log.warn("Dispute not found: {}", dispute.getId());
            }
        }
    }

    private String buildPlainTextResponse(List<MerchantDispute> disputes, Locale replyLocale) {
        return disputes.stream()
                .max(Comparator.comparing(MerchantDispute::getCreatedAt))
                .map(dispute -> prepareStatusMessage(dispute, replyLocale, polyglot))
                .orElseThrow(() -> new UnexpectedException("Disputes list cannot be empty!"));
    }

    private void updateDisputeInfo(MerchantDispute dispute, DisputeStatusResult disputeStatusResult) {
        dispute.setUpdatedAt(LocalDateTime.now());
        switch (disputeStatusResult.getSetField()) {
            case STATUS_PENDING -> dispute.setStatus(DisputeStatus.pending);
            case STATUS_SUCCESS -> {
                dispute.setStatus(DisputeStatus.approved);
                var disputeSuccess = disputeStatusResult.getStatusSuccess();
                if (disputeSuccess.getChangedAmount().isPresent()) {
                    dispute.setChangedAmount(disputeSuccess.getChangedAmount().get());
                }
            }
            case STATUS_FAIL -> {
                dispute.setStatus(DisputeStatus.declined);
                var mapping = disputeStatusResult.getStatusFail().getMapping();
                mapping.ifPresent(errorCode -> dispute.setMessage(explanation(polyglot, errorCode)));
            }
        }
    }
}
