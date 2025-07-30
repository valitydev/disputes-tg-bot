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
        assertTrue(merchantChatDao.get(chatId).isEmpty());

        var update = TelegramObjectUtil.buildAddMerchantChatCommand(chatId);
        update.getMessage().getChat().setId(adminChatProperties.getId());
        update.getMessage().setMessageThreadId(adminChatProperties.getTopics().getChatManagement());

        var chatInfo = TelegramObjectUtil.buildChatFullInfo(chatId);
        when(telegramClient.execute((GetChat) any())).thenReturn(chatInfo);
        assertDoesNotThrow(() -> disputesBot.consume(update));
        telegramEventHandlerList.forEach(handler -> {
            verify(handler, times(1)).filter(any());
        });
        assertTrue(merchantChatDao.get(chatId).isPresent());
    }
}
