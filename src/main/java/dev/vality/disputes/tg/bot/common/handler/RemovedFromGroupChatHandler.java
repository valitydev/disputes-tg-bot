package dev.vality.disputes.tg.bot.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
public class AddedToGroupChatHandler implements CommonHandler<Void> {

    private static final String status = "member";

    @Override
    public boolean filter(Update update) {
        boolean isNotMessage = update.hasMessage();
        boolean isMyGroupChatStatusChanged = update.hasMyChatMember();
        if (isNotMessage && isMyGroupChatStatusChanged) {
           boolean isMyRoleChanged = update.getMyChatMember().getNewChatMember() != null;
           if (isMyRoleChanged) {
               return status.equals(update.getMyChatMember().getNewChatMember().getStatus());
           }
        }
        return false;
    }

    @Override
    public Void handle(Update update, long userId) {
        log.debug("AddedToGroupChatHandler is handling update: {}", update);
        var chatMember = update.getMyChatMember();
        log.info("Added to chat: '{}' with id: '{}'", chatMember.getChat().getTitle(), chatMember.getChat().getId());
        return null;
    }
}
