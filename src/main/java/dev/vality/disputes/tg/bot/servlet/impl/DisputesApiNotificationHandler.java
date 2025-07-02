package dev.vality.disputes.tg.bot.servlet.impl;

import dev.vality.disputes.admin.*;
import dev.vality.disputes.tg.bot.handler.admin.notification.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesApiNotificationHandler implements AdminCallbackServiceSrv.Iface {

    private final NotificationHandler<DisputeAlreadyCreated> disputeAlreadyCreatedHandler;
    private final NotificationHandler<DisputeManualPending> disputeManualPendingHandler;
    private final NotificationHandler<DisputePoolingExpired> disputePoolingExpiredHandler;
    private final NotificationHandler<DisputeReadyForCreateAdjustment> disputeReadyForAdjustmentHandler;

    @Override
    public void notify(NotificationParamsRequest notificationParamsRequest) throws TException {
        log.info("Processing {} notifications", notificationParamsRequest.getNotifications().size());
        notificationParamsRequest.getNotifications().forEach(this::handle);
        log.info("All notifications were processed");
    }

    private void handle(Notification notification) {
        log.debug("Start processing '{}' notification: '{}'", notification.getSetField().getFieldName(), notification);
        switch (notification.getSetField()) {
            case DISPUTE_ALREADY_CREATED -> {
                var disputeAlreadyCreated = notification.getDisputeAlreadyCreated();
                disputeAlreadyCreatedHandler.handle(disputeAlreadyCreated);
            }
            case DISPUTE_POOLING_EXPIRED -> {
                var disputePoolingExpired = notification.getDisputePoolingExpired();
                disputePoolingExpiredHandler.handle(disputePoolingExpired);
            }
            case DISPUTE_READY_FOR_CREATE_ADJUSTMENT -> {
                var disputeReadyForAdjustment = notification.getDisputeReadyForCreateAdjustment();
                disputeReadyForAdjustmentHandler.handle(disputeReadyForAdjustment);
            }
            case DISPUTE_MANUAL_PENDING -> {
                var disputeManualPending = notification.getDisputeManualPending();
                disputeManualPendingHandler.handle(disputeManualPending);
            }
            default -> log.warn("Unknown notification type : {}", notification.getSetField().getFieldName());
        }
        log.debug("Finish processing '{}' notification", notification.getSetField().getFieldName());
    }
}
