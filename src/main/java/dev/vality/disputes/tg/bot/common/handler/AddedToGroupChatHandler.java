package dev.vality.disputes.tg.bot.common.handler;

import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;

@Slf4j
@Component
public class AddedToGroupChatHandler implements CommonHandler {


    @Override
    public boolean filter(MessageDto messageDto) {
        var update = messageDto.getUpdate();
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
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        var update = messageDto.getUpdate();
        log.debug("AddedToGroupChatHandler is handling update: {}", update);
        var chat = update.getMyChatMember().getChat();
        log.info("Bot was added to chat: '{}' with id: '{}'", chat.getTitle(), chat.getId());
    }
}
