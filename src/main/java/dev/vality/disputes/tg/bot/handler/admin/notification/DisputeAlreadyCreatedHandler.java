package dev.vality.disputes.tg.bot.handler.admin.notification;

import dev.vality.disputes.admin.DisputeAlreadyCreated;
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
public class DisputeAlreadyCreatedHandler implements NotificationHandler<DisputeAlreadyCreated> {

    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;

    @Override
    public void handle(DisputeAlreadyCreated notification) {
        String reply = polyglot.getText("dispute.support.already-created",
                FormatUtil.formatPaymentId(notification.getInvoiceId(), notification.getPaymentId()));
        telegramApiService.sendMessage(reply, adminChatProperties.getId(),
                adminChatProperties.getTopics().getReviewDisputesProcessing());
    }
}
