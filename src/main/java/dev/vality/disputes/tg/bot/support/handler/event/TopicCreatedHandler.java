package dev.vality.disputes.tg.bot.support.handler.event;

import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCreatedHandler implements SupportMessageHandler {

    private final SupportChatProperties supportChatProperties;
    private final TelegramClient telegramClient;
    private final Polyglot polyglot;


    @Override
    public boolean filter(Update update) {
        return update.hasMessage() && update.getMessage().getForumTopicCreated() != null;
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        var message = update.getMessage();
        String text = polyglot.getText("support.info.topic-created",
                message.getForumTopicCreated().getName(), message.getMessageThreadId());
        var response = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), text);
        response.setMessageThreadId(supportChatProperties.getTopics().getServiceInfo());
        telegramClient.execute(response);
    }
}
