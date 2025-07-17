package dev.vality.disputes.tg.bot.handler.provider;

import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.dto.ProviderMessageDto;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRootHandler implements TelegramEventHandler {

    private final List<ProviderMessageHandler> handlers;
    private final ProviderChatDao providerChatDao;

    @Override
    public boolean filter(Update message) {
        log.debug("Filtering inside ProviderRootHandler");
        Long chatId = TelegramUtil.getChatId(message);
        if (chatId == null) {
            return false;
        }
        Optional<ProviderChat> chat = providerChatDao.get(chatId);
        log.debug("ProviderRootHandler filtering result: {}", chat.isPresent());
        return chat.isPresent();
    }


    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update message) {
        Long chatId = TelegramUtil.getChatId(message);
        Optional<ProviderChat> chat = providerChatDao.get(chatId);
        if (chat.isEmpty()) {
            return;
        }
        var providerMessage = ProviderMessageDto.builder()
                .providerChat(chat.get())
                .update(message)
                .build();

        handlers.stream()
                .filter(handler -> handler.filter(providerMessage))
                .findFirst()
                .ifPresentOrElse(
                        handler -> {
                            try {
                                handler.handle(providerMessage);
                            } catch (Exception e) {
                                log.error("Unexpected exception occurred: ", e);
                            }
                        },
                        () -> log.info("[{}] No suitable provider handler found for update: {}",
                                providerMessage.getUpdate().getUpdateId(),
                                providerMessage));
    }
}
