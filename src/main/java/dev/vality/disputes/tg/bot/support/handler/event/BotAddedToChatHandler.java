package dev.vality.disputes.tg.bot.support.handler.event;

import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotAddedToChatHandler implements SupportMessageHandler {

    private final SupportChatProperties supportChatProperties;
    private final TelegramClient telegramClient;


    @Override
    public boolean filter(Update update) {
        boolean isMyGroupChatStatusChanged = update.hasMyChatMember();
        if (!update.hasMessage() && isMyGroupChatStatusChanged) {
            boolean isMyRoleChanged = update.getMyChatMember().getNewChatMember() != null;
            if (isMyRoleChanged) {
                return update.getMyChatMember().getNewChatMember() instanceof ChatMemberMember
                        || update.getMyChatMember().getNewChatMember() instanceof ChatMemberAdministrator;
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.debug("BotAddedToChatHandler is handling update: {}", update);
        var chat = update.getMyChatMember().getChat();
        log.info("Bot was added to chat: '{}' with id: '{}'", chat.getTitle(), chat.getId());
        String text =
                ("""
                        ➕Бот добавлен в чат
                         Название чата: <code>%s</code>
                         Идентификатор чата: <code>%d</code>
                        """
                ).formatted(chat.getTitle(), chat.getId());
        var response = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), text);
        response.setMessageThreadId(supportChatProperties.getTopics().getServiceInfo());
        telegramClient.execute(response);
    }
}
