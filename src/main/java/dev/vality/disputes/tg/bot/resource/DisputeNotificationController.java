package dev.vality.disputes.tg.bot.resource;

import tools.jackson.databind.ObjectMapper;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.handler.merchant.command.StatusDisputeHandler;
import dev.vality.disputes.tg.bot.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DisputeNotificationController {

    private final ObjectMapper objectMapper;
    private final MerchantDisputeDao merchantDisputeDao;
    private final StatusDisputeHandler statusDisputeHandler;
    private final TelegramNotificationService telegramNotificationService;

    @PostMapping("/v1/disputes/notifications")
    public void notify(@RequestBody(required = false) String body) {
        log.info("Received dispute notification: {}", body);
        try {
            var notification = objectMapper.readTree(body);
            var disputeId = UUID.fromString(notification.path("disputeId").asText());
            var dispute = merchantDisputeDao.getByDisputeId(disputeId.toString()).orElseThrow();
            if (!dispute.getInvoiceId().equals(notification.path("invoiceId").asText())
                    || !dispute.getPaymentId().equals(notification.path("paymentId").asText())) {
                throw new IllegalArgumentException("Payment does not match dispute " + disputeId);
            }

            var previousStatus = dispute.getStatus();
            statusDisputeHandler.updateDisputesStatuses(List.of(dispute));
            if (previousStatus == DisputeStatus.pending && dispute.getStatus() != DisputeStatus.pending) {
                telegramNotificationService.sendDisputeStatusToMerchant(dispute);
            }
        } catch (Exception e) {
            log.error("Failed to process dispute notification", e);
        }
    }
}
