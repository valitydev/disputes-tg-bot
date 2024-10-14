package dev.vality.disputes.tg.bot.merchant.service;

import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TelegramUtil;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.merchant.handler.StatusDisputeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@ConditionalOnProperty(value = "dispute.isScheduleEnabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScheduledDisputesStatusCheckerService {

    private final Polyglot polyglot;
    private final DisputesBot disputesBot;
    private final MerchantDisputeDao merchantDisputeDao;
    private final StatusDisputeHandler statusDisputeHandler;
    @Value("${dispute.batchSize}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${dispute.fixedDelayStatus}", initialDelayString = "${dispute.initialDelayStatus}")
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
        String reply = createDisputeStatusInfoResponse(dispute, dispute.getTgMessageLocale());
        try {
            disputesBot.execute(TelegramUtil.buildPlainTextResponse(dispute.getChatId(),
                    reply, Math.toIntExact(dispute.getTgMessageId())));
        } catch (Exception ex) {
            log.warn("Failed to send reply, dispute: {}", dispute, ex);
        }
    }

    private String createDisputeStatusInfoResponse(MerchantDispute dispute, String replyLocale) {
        return polyglot.getText(replyLocale, "dispute.status",
                dispute.getDisputeId(),
                dispute.getInvoiceId(),
                dispute.getExternalId(),
                dispute.getStatus().getLiteral(),
                dispute.getUpdatedAt());
    }
}
