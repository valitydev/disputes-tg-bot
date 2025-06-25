package dev.vality.disputes.tg.bot.merchant.handler.inner;

import dev.vality.disputes.tg.bot.core.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.core.event.NewMerchantChat;
import dev.vality.disputes.tg.bot.core.handler.InternalEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddNewChatHandler implements InternalEventHandler<NewMerchantChat> {

    private final MerchantChatDao merchantChatDao;

    @Override
    @EventListener
    public void handle(NewMerchantChat event) {
        MerchantChat merchantChat = new MerchantChat();
        merchantChat.setChatId(event.getChatFullInfo().getId());
        merchantChat.setTitle(event.getChatFullInfo().getTitle());
        long chatId = merchantChatDao.save(merchantChat);
        log.info("Chat saved successfully with id: {}", chatId);
    }
}
