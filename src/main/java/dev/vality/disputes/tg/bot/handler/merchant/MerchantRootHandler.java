package dev.vality.disputes.tg.bot.handler.merchant;

import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantRootHandler implements TelegramEventHandler {

    private final List<MerchantMessageHandler> handlers;
    private final MerchantChatDao merchantChatDao;

    @Override
    public boolean filter(Update message) {
        Long chatId = TelegramUtil.getChatId(message);
        if (chatId == null) {
            return false;
        }
        Optional<MerchantChat> chat = merchantChatDao.get(chatId);
        return chat.isPresent();
    }


    @Override
    @SuppressWarnings({"DataFlowIssue", "OptionalGetWithoutIsPresent"})
    public void handle(Update message) {
        Long chatId = TelegramUtil.getChatId(message);
        Optional<MerchantChat> chat = merchantChatDao.get(chatId);
        var merchantMessage = MerchantMessageDto.builder()
                .merchantChat(chat.get())
                .update(message)
                .build();

        handlers.stream()
                .filter(handler -> handler.filter(merchantMessage))
                .findFirst()
                .ifPresentOrElse(
                        handler -> {
                            try {
                                handler.handle(merchantMessage);
                            } catch (Exception e) {
                                log.error("Unexpected exception occurred: ", e);
                            }
                        },
                        () -> log.info("[{}] No suitable merchant handler found for update: {}",
                                merchantMessage.getUpdate().getUpdateId(),
                                merchantMessage));
    }
}
