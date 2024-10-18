package dev.vality.disputes.tg.bot.common.handler;

import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft;

@Slf4j
@Component
public class RemovedFromGroupChatHandler implements CommonHandler {


    @Override
    public boolean filter(MessageDto messageDto) {
        var update = messageDto.getUpdate();
        boolean isMyGroupChatStatusChanged = update.hasMyChatMember();
        if (!update.hasMessage() && isMyGroupChatStatusChanged) {
            boolean isMyRoleChanged = update.getMyChatMember().getNewChatMember() != null;
            if (isMyRoleChanged) {
                return update.getMyChatMember().getNewChatMember() instanceof ChatMemberLeft;
            }
        }
        return false;
    }

    @Override
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        var update = messageDto.getUpdate();
        log.debug("RemovedFromGroupChatHandler is handling update: {}", update);
        var chatMember = update.getMyChatMember();
        log.info("Bot was removed from chat: '{}' with id: '{}'", chatMember.getChat().getTitle(),
                chatMember.getChat().getId());
    }
}
