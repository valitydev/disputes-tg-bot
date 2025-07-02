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
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotRemovedFromChatHandler implements AdminMessageHandler {

    private final AdminChatProperties adminChatProperties;
    private final TelegramClient telegramClient;
    private final Polyglot polyglot;

    @Override
    public boolean filter(Update update) {
        boolean isMyGroupChatStatusChanged = update.hasMyChatMember();
        if (!update.hasMessage() && isMyGroupChatStatusChanged) {
            boolean isMyRoleChanged = update.getMyChatMember().getNewChatMember() != null;
            if (isMyRoleChanged) {
                return update.getMyChatMember().getNewChatMember() instanceof ChatMemberLeft
                        || update.getMyChatMember().getNewChatMember() instanceof ChatMemberBanned;
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.debug("RemovedFromGroupChatHandler is handling update: {}", update);
        var chat = update.getMyChatMember().getChat();
        log.info("Bot was removed from chat: '{}' with id: '{}'", chat.getTitle(),
                chat.getId());
        String text = polyglot.getText("support.info.bot-removed-from-chat",
                chat.getTitle(), chat.getId());
        var response = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), text);
        response.setMessageThreadId(adminChatProperties.getTopics().getServiceInfo());
        telegramClient.execute(response);
    }
}
