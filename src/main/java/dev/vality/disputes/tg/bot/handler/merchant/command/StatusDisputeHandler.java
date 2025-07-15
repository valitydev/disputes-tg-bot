package dev.vality.disputes.tg.bot.handler.merchant.command;

import dev.vality.disputes.merchant.DisputeContext;
import dev.vality.disputes.merchant.DisputeNotFound;
import dev.vality.disputes.merchant.DisputeStatusResult;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.dto.command.StatusDisputeCommand;
import dev.vality.disputes.tg.bot.exception.UnexpectedException;
import dev.vality.disputes.tg.bot.handler.merchant.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.StatusDisputeCommandParser;
import dev.vality.disputes.tg.bot.service.external.DisputesApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.util.PolyglotUtil.explanation;
import static dev.vality.disputes.tg.bot.util.PolyglotUtil.prepareStatusMessage;
import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("MissingSwitchDefault")
public class StatusDisputeHandler implements MerchantMessageHandler {

    private final MerchantDisputeDao merchantDisputeDao;
    private final StatusDisputeCommandParser commandParser;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;
    private final TelegramApiService telegramApiService;

    @Override
    public boolean filter(MerchantMessageDto message) {
        String messageText = extractText(message.getUpdate());
        return commandParser.canParse(messageText);
    }

    @Override
    public void handle(MerchantMessageDto message) throws TelegramApiException {
        Update update = message.getUpdate();
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale(message.getMerchantChat().getLocale());
        
        StatusDisputeCommand command = commandParser.parse(messageText);
        
        if (command.hasValidationError()) {
            log.warn("Command validation failed: {}", command.getValidationError().getMessageKey());
            String reply = polyglot.getText(replyLocale, command.getValidationError().getMessageKey());
            telegramApiService.sendReplyTo(reply, update);
            return;
        }

        List<MerchantDispute> matchingDisputes = findDisputes(command.getDisputeInfo());
        if (matchingDisputes.isEmpty()) {
            log.info("Dispute not found: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.dispute-not-found");
            telegramApiService.sendReplyTo(reply, update);
            return;
        }
        updateDisputesStatuses(matchingDisputes);
        String response = buildPlainTextResponse(matchingDisputes, replyLocale);
        telegramApiService.sendReplyTo(response, update);
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
