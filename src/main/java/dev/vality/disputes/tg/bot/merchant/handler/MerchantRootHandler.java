package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.core.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.merchant.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.merchant.dto.MerchantMessageDto;
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
                        handler -> handler.handle(merchantMessage),
                        () -> log.info("[{}] No suitable merchant handler found for update: {}",
                                merchantMessage.getUpdate().getUpdateId(),
                                merchantMessage));
    }
}
