package dev.vality.disputes.tg.bot.handler.admin.event;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCreatedHandler implements AdminMessageHandler {

    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
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
        telegramApiService.sendMessage(text, adminChatProperties.getId(),
                adminChatProperties.getTopics().getServiceInfo());
    }
}
