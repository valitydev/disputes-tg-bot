package dev.vality.disputes.tg.bot.handler.inner;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.event.NewMerchantChat;
import dev.vality.disputes.tg.bot.handler.InternalEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddNewChatHandler implements InternalEventHandler<NewMerchantChat> {

    private final MerchantChatDao merchantChatDao;

    @Override
    @TransactionalEventListener
    @Transactional
    public void handle(NewMerchantChat event) {
        MerchantChat merchantChat = new MerchantChat();
        merchantChat.setChatId(event.getChatFullInfo().getId());
        merchantChat.setTitle(event.getChatFullInfo().getTitle());
        long chatId = merchantChatDao.save(merchantChat);
        log.info("Chat saved successfully with id: {}", chatId);
    }
}
