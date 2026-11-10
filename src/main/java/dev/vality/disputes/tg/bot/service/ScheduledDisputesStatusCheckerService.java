package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.handler.merchant.command.StatusDisputeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@ConditionalOnProperty(value = "dispute.isScheduleEnabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScheduledDisputesStatusCheckerService {

    private final MerchantDisputeDao merchantDisputeDao;
    private final StatusDisputeHandler statusDisputeHandler;
    private final TelegramNotificationService telegramNotificationService;
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
                return;
            } else {
                log.info("Found {} pending disputes", disputes.size());
            }
            statusDisputeHandler.updateDisputesStatuses(disputes);
            List<MerchantDispute> finalizedDisputes =
                    disputes.stream().filter(dispute -> !DisputeStatus.pending.equals(dispute.getStatus()))
                            .toList();
            if (!finalizedDisputes.isEmpty()) {
                log.info("Finalized {} disputes by schedule", finalizedDisputes.size());
                finalizedDisputes.forEach(telegramNotificationService::sendDisputeStatusToMerchant);
            }
        } catch (Exception ex) {
            log.error("Received exception while scheduled processing created disputes", ex);
        }
    }

}
