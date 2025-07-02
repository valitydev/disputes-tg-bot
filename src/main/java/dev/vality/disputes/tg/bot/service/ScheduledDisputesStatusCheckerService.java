package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.core.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.handler.command.StatusDisputeHandler;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static dev.vality.disputes.tg.bot.util.PolyglotUtil.prepareStatusMessage;

@Slf4j
@ConditionalOnProperty(value = "dispute.isScheduleEnabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScheduledDisputesStatusCheckerService {

    private final Polyglot polyglot;
    private final TelegramClient telegramClient;
    private final MerchantDisputeDao merchantDisputeDao;
    private final MerchantChatDao merchantChatDao;
    private final StatusDisputeHandler statusDisputeHandler;
    @Value("${dispute.batchSize}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${dispute.fixedDelayStatus}", initialDelayString = "${dispute.initialDelayStatus}")
    @Transactional
    public void processPendingDisputes() {
        log.debug("Updating pending disputes statuses");
        try {
            var disputes = merchantDisputeDao.getPendingDisputesSkipLocked(batchSize);
            if (disputes.isEmpty()) {
                log.debug("Found 0 pending disputes");
            } else {
                log.info("Found {} pending disputes", disputes.size());
            }
            statusDisputeHandler.updateDisputesStatuses(disputes);
            List<MerchantDispute> finalizedDisputes =
                    disputes.stream().filter(dispute -> !DisputeStatus.pending.equals(dispute.getStatus()))
                            .toList();
            log.info("Finalized {} disputes by schedule", finalizedDisputes.size());
            finalizedDisputes.forEach(this::sendReply);
        } catch (Exception ex) {
            log.error("Received exception while scheduled processing created disputes", ex);
        }
    }

    private void sendReply(MerchantDispute dispute) {
        var chat = merchantChatDao.getById(dispute.getChatId()).get();
        var locale = polyglot.getLocale(chat.getLocale());
        String reply = prepareStatusMessage(dispute, locale, polyglot);
        try {
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(chat.getChatId(),
                    reply, Math.toIntExact(dispute.getTgMessageId())));
        } catch (Exception ex) {
            log.warn("Failed to send reply, dispute: {}", dispute, ex);
        }
    }
}
