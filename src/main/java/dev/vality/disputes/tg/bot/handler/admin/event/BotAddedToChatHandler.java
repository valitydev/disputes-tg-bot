package dev.vality.disputes.tg.bot.handler.admin.event;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotAddedToChatHandler implements AdminMessageHandler {

    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;

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
    public void handle(Update update) {
        log.debug("BotAddedToChatHandler is handling update: {}", update);
        var chat = update.getMyChatMember().getChat();
        log.info("Bot was added to chat: '{}' with id: '{}'", chat.getTitle(), chat.getId());

        String text = polyglot.getText("support.info.bot-added-to-chat", chat.getTitle(), chat.getId());
        telegramApiService.sendMessage(text, adminChatProperties.getId(),
                adminChatProperties.getTopics().getServiceInfo());
    }
}
