package dev.vality.disputes.tg.bot.handler.admin;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRootHandler implements TelegramEventHandler {

    private final List<AdminMessageHandler> handlers;
    private final AdminChatProperties adminChatProperties;

    @Override
    public boolean filter(Update message) {
        Long chatId = TelegramUtil.getChatId(message);
        return isSupportChat(chatId) || isBotMembershipStatusChanged(message);
    }


    @Override
    public void handle(Update message) {
        handlers.stream()
                .filter(handler -> handler.filter(message))
                .findFirst()
                .ifPresentOrElse(
                        handler -> {
                            try {
                                handler.handle(message);
                            } catch (Exception e) {
                                log.error("Unexpected exception occurred: ", e);
                            }
                        },
                        () -> log.info("[{}] No suitable support handler found for update: {}",
                                message.getUpdateId(),
                                message));
    }

    private boolean isSupportChat(Long chatId) {
        return adminChatProperties.getId().equals(chatId);
    }

    private boolean isBotMembershipStatusChanged(Update update) {
        return update.hasMyChatMember();
    }
}
