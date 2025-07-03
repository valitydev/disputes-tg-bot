package dev.vality.disputes.tg.bot.handler.admin.notification;

import dev.vality.disputes.admin.DisputeReadyForCreateAdjustment;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeReadyForAdjustmentHandler implements NotificationHandler<DisputeReadyForCreateAdjustment> {

    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final AdminChatProperties adminChatProperties;

    @Override
    public void handle(DisputeReadyForCreateAdjustment notification) {
        String reply = polyglot.getText("dispute.support.adjustment-ready",
                FormatUtil.formatPaymentId(notification.getInvoiceId(), notification.getPaymentId()));
        telegramApiService.sendMessage(reply, adminChatProperties.getId(),
                adminChatProperties.getTopics().getReviewDisputesProcessing());
    }
}
