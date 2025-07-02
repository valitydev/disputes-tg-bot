package dev.vality.disputes.tg.bot.handler.admin.event;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCreatedHandler implements AdminMessageHandler {

    private final AdminChatProperties adminChatProperties;
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
        var response = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), text);
        response.setMessageThreadId(adminChatProperties.getTopics().getServiceInfo());
        telegramClient.execute(response);
    }
}
