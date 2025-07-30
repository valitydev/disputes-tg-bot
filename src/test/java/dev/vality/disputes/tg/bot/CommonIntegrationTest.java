package dev.vality.disputes.tg.bot;

import dev.vality.disputes.tg.bot.config.PostgresqlSpringBootITest;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.service.DisputesBot;
import dev.vality.disputes.tg.bot.test.TelegramObjectUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@PostgresqlSpringBootITest
@SpringBootTest(
        properties = {"admin.chat.id=1",
                "admin.chat.topics.chat-management=2"})
public class CommonIntegrationTest {

    @Autowired
    private DisputesBot disputesBot;
    @MockitoBean
    private TelegramBotInitializer telegramBotInitializer;
    @MockitoBean
    private List<TelegramEventHandler> telegramEventHandlerList;
    @MockitoBean
    private TelegramClient telegramClient;
    @Autowired
    private MerchantChatDao merchantChatDao;
    @Autowired
    private AdminChatProperties adminChatProperties;

    @Test
    @DisplayName("Simple message from random chat doesn't pass any filter")
    public void testRandomMessageDoesNotTriggerAnything() {
        var update = TelegramObjectUtil.buildSimpleTextMessage();
        assertDoesNotThrow(() -> disputesBot.consume(update));
        telegramEventHandlerList.forEach(handler -> {
            verify(handler, times(1)).filter(any());
            verify(handler, times(0)).handle(any());
        });
        verifyNoInteractions(telegramClient);
    }

    @SneakyThrows
    @Test
    @DisplayName("Add merchant chat command success")
    public void testAddMerchantChatCommandSuccess() {
        long chatId = new Random().nextLong();
        addMerchantChatSuccess(chatId);
    }

    private void addMerchantChatSuccess(long chatId) throws TelegramApiException {
        // First add a merchant chat
        var addUpdate = TelegramObjectUtil.buildAddMerchantChatCommand(chatId);
        addUpdate.getMessage().getChat().setId(adminChatProperties.getId());
        addUpdate.getMessage().setMessageThreadId(adminChatProperties.getTopics().getChatManagement());

        var chatInfo = TelegramObjectUtil.buildChatFullInfo(chatId);
        when(telegramClient.execute((GetChat) any())).thenReturn(chatInfo);
        assertDoesNotThrow(() -> disputesBot.consume(addUpdate));

        // Verify chat was added and is enabled
        var addedChat = merchantChatDao.get(chatId);
        assertTrue(addedChat.isPresent());
    }

    @SneakyThrows
    @Test
    @DisplayName("Disable merchant chat command success")
    public void testDisableMerchantChatCommandSuccess() {
        long chatId = new Random().nextLong();
        
        // First add a merchant chat
        addMerchantChatSuccess(chatId);

        // Disable the merchant chat
        var disableUpdate = TelegramObjectUtil.buildDisableMerchantChatCommand(chatId);
        disableUpdate.getMessage().getChat().setId(adminChatProperties.getId());
        disableUpdate.getMessage().setMessageThreadId(adminChatProperties.getTopics().getChatManagement());

        assertDoesNotThrow(() -> disputesBot.consume(disableUpdate));
        telegramEventHandlerList.forEach(handler -> {
            verify(handler, atLeast(1)).filter(any());
        });
        
        // Verify chat was disabled (get() method filters only enabled chats)
        var disabledChat = merchantChatDao.get(chatId);
        assertTrue(disabledChat.isEmpty());
    }
}
