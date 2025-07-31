package dev.vality.disputes.tg.bot;

import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.merchant.DisputeCreatedResult;
import dev.vality.disputes.merchant.DisputeCreatedSuccessResult;
import dev.vality.disputes.merchant.DisputeStatusResult;
import dev.vality.disputes.tg.bot.config.PostgresqlSpringBootITest;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.service.DisputesBot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DisputesApiService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import dev.vality.disputes.tg.bot.test.TelegramObjectUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
    @MockitoBean
    private DisputesApiService disputesApiService;
    @MockitoBean
    private HellgateService hellgateService;
    @Autowired
    private MerchantChatDao merchantChatDao;
    @Autowired
    private MerchantDisputeDao merchantDisputeDao;
    @Autowired
    private AdminChatProperties adminChatProperties;
    @MockitoSpyBean
    private TelegramApiService telegramApiService;

    // Test constants
    private static final String TEST_PAYMENT_ID = "1";
    private static final String TEST_DISPUTE_ID = UUID.randomUUID().toString();

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
        long chatId = TelegramObjectUtil.generateRandomChatId();
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
        long chatId = TelegramObjectUtil.generateRandomChatId();
        
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

    @SneakyThrows
    @Test
    @DisplayName("Create dispute command success")
    public void testCreateDisputeCommandSuccess() {
        long chatId = TelegramObjectUtil.generateRandomChatId();
        
        // First add a merchant chat
        addMerchantChatSuccess(chatId);

        // Mock external services
        var invoice = new Invoice();
        var invoiceInner = new dev.vality.damsel.domain.Invoice();
        invoiceInner.setId(TelegramObjectUtil.TEST_INVOICE_ID);
        invoice.setInvoice(invoiceInner);
        
        var payment = new InvoicePayment();
        var paymentInner = new dev.vality.damsel.domain.InvoicePayment();
        paymentInner.setId(TEST_PAYMENT_ID);
        payment.setPayment(paymentInner);
        invoice.setPayments(List.of(payment));
        
        when(hellgateService.getInvoice(TelegramObjectUtil.TEST_INVOICE_ID)).thenReturn(invoice);
        
        var disputeResult = new DisputeCreatedResult();
        var successResult = new DisputeCreatedSuccessResult();
        successResult.setDisputeId(TEST_DISPUTE_ID);
        disputeResult.setSuccessResult(successResult);
        when(disputesApiService.createDispute(any())).thenReturn(disputeResult);

        // Create dispute command with attachment
        var update = TelegramObjectUtil.buildMerchantCreateDisputeCommand();
        update.getMessage().getChat().setId(chatId);

        // Check that no dispute exists before
        var disputesBefore = merchantDisputeDao.getByInvoiceId(TelegramObjectUtil.TEST_INVOICE_ID);
        assertTrue(disputesBefore.isEmpty());

        doReturn(new byte[0]).when(telegramApiService).getFile(any());
        // Execute command
        assertDoesNotThrow(() -> disputesBot.consume(update));

        // Verify dispute was created
        var disputesAfter = merchantDisputeDao.getByInvoiceId(TelegramObjectUtil.TEST_INVOICE_ID);
        assertFalse(disputesAfter.isEmpty());
        assertEquals(1, disputesAfter.size());
        assertEquals(TEST_DISPUTE_ID, disputesAfter.get(0).getId().toString());
        assertEquals(TelegramObjectUtil.TEST_INVOICE_ID, disputesAfter.get(0).getInvoiceId());
        assertEquals(TEST_PAYMENT_ID, disputesAfter.get(0).getPaymentId());
        
        // Verify telegram client was called to send reply to user
        verify(telegramClient, times(1)).execute(any(SendMessage.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Status dispute command success for existing dispute")
    public void testStatusDisputeCommandSuccess() {
        long chatId = TelegramObjectUtil.generateRandomChatId();
        
        // First add a merchant chat and create a dispute
        addMerchantChatSuccess(chatId);
        createDisputeForStatusTest(chatId);

        // Mock dispute status result - create a simple mock
        var statusResult = mock(DisputeStatusResult.class);
        when(statusResult.getSetField()).thenReturn(DisputeStatusResult._Fields.STATUS_PENDING);
        when(disputesApiService.getStatus(any())).thenReturn(statusResult);

        // Create status dispute command
        var update = TelegramObjectUtil.buildMerchantStatusDisputeCommand();
        update.getMessage().getChat().setId(chatId);

        // Execute command
        assertDoesNotThrow(() -> disputesBot.consume(update));

        // Verify status request was made
        verify(disputesApiService, times(1)).getStatus(any());
        
        // Verify telegram client was called to send reply to user
        verify(telegramClient, times(2)).execute(any(SendMessage.class)); // 1 for create + 1 for status
    }

    @SneakyThrows
    @Test
    @DisplayName("Status dispute command for non-existing dispute")
    public void testStatusDisputeCommandForNonExistingDispute() {
        long chatId = TelegramObjectUtil.generateRandomChatId();
        
        // First add a merchant chat
        addMerchantChatSuccess(chatId);

        // Create status dispute command for non-existing dispute
        var update = TelegramObjectUtil.buildMerchantStatusNonExistingDisputeCommand();
        update.getMessage().getChat().setId(chatId);

        // Execute command
        assertDoesNotThrow(() -> disputesBot.consume(update));

        // Verify disputesApiService.getStatus was never called because dispute wasn't found in DB
        verify(disputesApiService, never()).getStatus(any());
        
        // Verify telegram client was called to send error reply to user
        verify(telegramClient, times(1)).execute(any(SendMessage.class));
    }

    private void createDisputeForStatusTest(long chatId) throws TelegramApiException, InvoiceNotFound, IOException {
        // Mock external services
        var invoice = new Invoice();
        var invoiceInner = new dev.vality.damsel.domain.Invoice();
        invoiceInner.setId(TelegramObjectUtil.TEST_INVOICE_ID);
        invoice.setInvoice(invoiceInner);
        
        var payment = new InvoicePayment();
        var paymentInner = new dev.vality.damsel.domain.InvoicePayment();
        paymentInner.setId(TEST_PAYMENT_ID);
        payment.setPayment(paymentInner);
        invoice.setPayments(List.of(payment));
        
        when(hellgateService.getInvoice(TelegramObjectUtil.TEST_INVOICE_ID)).thenReturn(invoice);
        
        var disputeResult = new DisputeCreatedResult();
        var successResult = new DisputeCreatedSuccessResult();
        successResult.setDisputeId(TEST_DISPUTE_ID);
        disputeResult.setSuccessResult(successResult);
        when(disputesApiService.createDispute(any())).thenReturn(disputeResult);

        // Create dispute command with attachment
        var update = TelegramObjectUtil.buildMerchantCreateDisputeCommand();
        update.getMessage().getChat().setId(chatId);

        doReturn(new byte[0]).when(telegramApiService).getFile(any());
        // Execute command
        assertDoesNotThrow(() -> disputesBot.consume(update));

        // Verify dispute was created
        var disputesAfter = merchantDisputeDao.getByInvoiceId(TelegramObjectUtil.TEST_INVOICE_ID);
        assertFalse(disputesAfter.isEmpty());
    }
}
