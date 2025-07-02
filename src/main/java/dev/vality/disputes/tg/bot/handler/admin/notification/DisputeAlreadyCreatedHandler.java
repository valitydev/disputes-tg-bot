package dev.vality.disputes.tg.bot.handler.admin.notification;

import dev.vality.disputes.admin.DisputeAlreadyCreated;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeAlreadyCreatedHandler implements NotificationHandler<DisputeAlreadyCreated> {

    private final TelegramClient telegramClient;
    private final Polyglot polyglot;
    private final AdminChatProperties adminChatProperties;

    @Override
    @SneakyThrows
    public void handle(DisputeAlreadyCreated notification) {
        String reply = polyglot.getText("dispute.support.already-created",
                FormatUtil.formatPaymentId(notification.getInvoiceId(), notification.getPaymentId()));
        var messageToSupport = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), reply);
        messageToSupport.setMessageThreadId(adminChatProperties.getTopics().getReviewDisputesProcessing());
        telegramClient.execute(messageToSupport);
    }
}
