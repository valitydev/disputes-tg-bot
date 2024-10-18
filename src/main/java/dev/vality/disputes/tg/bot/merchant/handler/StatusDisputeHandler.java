package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.disputes.merchant.DisputeContext;
import dev.vality.disputes.merchant.DisputeNotFound;
import dev.vality.disputes.merchant.DisputeStatusResult;
import dev.vality.disputes.tg.bot.common.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TelegramUtil;
import dev.vality.disputes.tg.bot.common.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.merchant.service.external.DisputesApiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.extractText;
import static dev.vality.disputes.tg.bot.merchant.util.PolyglotUtil.prepareStatusMessage;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("MissingSwitchDefault")
public class StatusDisputeHandler implements CommonHandler {

    private static final String LATIN_SINGLE_LETTER = "/s ";
    private static final String FULL_COMMAND = "/status ";

    private final MerchantDisputeDao merchantDisputeDao;
    private final Polyglot polyglot;
    private final DisputesApiService disputesApiService;

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
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @SneakyThrows
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        Update update = messageDto.getUpdate();
        String messageText = extractText(update);
        String replyLocale = update.getMessage().getFrom().getLanguageCode();
        Optional<DisputeInfoDto> paymentInfoOptional = TextParsingUtil.getDisputeInfo(messageText);

        if (paymentInfoOptional.isEmpty()) {
            log.warn("Payment info not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.dispute-or-invoice-missing");
            disputesBot.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }

        List<MerchantDispute> matchingDisputes = findDisputes(paymentInfoOptional.get());
        if (matchingDisputes.isEmpty()) {
            log.info("Dispute not found: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.dispute-not-found");
            disputesBot.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }
        updateDisputesStatuses(matchingDisputes);
        String response;
        if (matchingDisputes.isEmpty()) {
            response = polyglot.getText(replyLocale, "error.input.dispute-not-found");
        } else {
            response = buildPlainTextResponse(matchingDisputes, replyLocale);
        }
        disputesBot.execute(TelegramUtil.buildPlainTextResponse(update, response));
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
            disputeContext.setDisputeId(dispute.getDisputeId());
            DisputeStatusResult result;
            try {
                result = disputesApiService.getStatus(disputeContext);
                updateDisputeInfo(dispute, result);
                merchantDisputeDao.update(dispute);
            } catch (DisputeNotFound e) {
                log.warn("Dispute not found: {}", dispute.getDisputeId());
            }
        }
    }

    private String buildPlainTextResponse(List<MerchantDispute> disputes, String replyLocale) {
        return disputes.stream()
                .map(dispute -> prepareStatusMessage(dispute, replyLocale, polyglot))
                .collect(Collectors.joining("\n\n"));
    }

    private void updateDisputeInfo(MerchantDispute dispute, DisputeStatusResult disputeStatusResult) {
        dispute.setUpdatedAt(LocalDateTime.now());
        switch (disputeStatusResult.getSetField()) {
            case STATUS_PENDING -> dispute.setStatus(DisputeStatus.pending);
            case STATUS_SUCCESS -> dispute.setStatus(DisputeStatus.approved);
            case STATUS_FAIL -> {
                dispute.setStatus(DisputeStatus.declined);
                dispute.setMessage(disputeStatusResult.getStatusFail().getMessage());
            }
        }
    }
}
