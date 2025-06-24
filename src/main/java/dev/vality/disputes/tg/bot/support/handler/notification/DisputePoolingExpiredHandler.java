package dev.vality.disputes.tg.bot.support.handler.notification;

import dev.vality.disputes.admin.DisputePoolingExpired;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.FormatUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputePoolingExpiredHandler implements NotificationHandler<DisputePoolingExpired> {

    private final TelegramClient telegramClient;
    private final Polyglot polyglot;
    private final SupportChatProperties supportChatProperties;

    @Override
    @SneakyThrows
    public void handle(DisputePoolingExpired notification) {
        //TODO: text check
        String reply = polyglot.getText("dispute.support.pooling-expired",
                FormatUtil.formatPaymentId(notification.getInvoiceId(), notification.getPaymentId()));
        var messageToSupport = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), reply);
        messageToSupport.setMessageThreadId(supportChatProperties.getTopics().getReviewDisputesProcessing());
        telegramClient.execute(messageToSupport);
    }
}
