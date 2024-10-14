package dev.vality.disputes.tg.bot.support.handler;

import dev.vality.disputes.tg.bot.core.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportRootHandler implements TelegramEventHandler {

    private final List<SupportMessageHandler> handlers;
    private final SupportChatProperties supportChatProperties;

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
                        handler -> handler.handle(message),
                        () -> log.info("[{}] No suitable support handler found for update: {}",
                                message.getUpdateId(),
                                message));
    }

    private boolean isSupportChat(Long chatId) {
        return supportChatProperties.getId().equals(chatId);
    }

    private boolean isBotMembershipStatusChanged(Update update) {
        return update.hasMyChatMember();
    }
}
