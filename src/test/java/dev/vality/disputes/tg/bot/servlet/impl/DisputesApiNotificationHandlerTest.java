package dev.vality.disputes.tg.bot.servlet.impl;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.domain.Provider;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.TerminalRef;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.admin.Attachment;
import dev.vality.disputes.admin.Dispute;
import dev.vality.disputes.tg.bot.config.ResponseDictionaryConfig;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.config.properties.DictionaryProperties;
import dev.vality.disputes.tg.bot.converter.AdminDisputeAttachmentConverter;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.AdminDisputeReview;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
@SpringJUnitConfig(classes = {
        DisputesApiNotificationHandler.class,
        Polyglot.class,
        ResponseDictionaryConfig.class,
        DisputesApiNotificationHandlerTest.TestConfig.class
})
class DisputesApiNotificationHandlerTest {

    private static final String INVOICE_ID = "invoice-1";
    private static final String PAYMENT_ID = "payment-1";
    private static final UUID DISPUTE_ID = UUID.fromString("90ed2701-8a0d-488e-9ba6-c575f4f2f4e0");
    private static final String PROVIDER_TRX_ID = "trx-1";
    private static final long TG_MESSAGE_ID = 123L;

    @MockitoBean
    private TelegramApiService telegramApiService;
    @MockitoBean
    private DominantService dominantService;
    @MockitoBean
    private HellgateService hellgateService;
    @MockitoBean
    private AdminDisputeReviewDao adminDisputeReviewDao;
    @MockitoBean
    private AdminDisputeAttachmentConverter attachmentConverter;
    private ArgumentCaptor<AdminDisputeReview> reviewCaptor;
    private ArgumentCaptor<String> messageCaptor;

    @Autowired
    private AdminChatProperties adminChatProperties;
    @Autowired
    private Polyglot polyglot;
    @Autowired
    private DisputesApiNotificationHandler notificationHandler;
    @MockitoBean
    private DictionaryProperties dictionaryProperties;
    @MockitoBean(name = "providerResponseDictionary")
    private List<?> providerResponseDictionary;

    @Configuration
    static class TestConfig {
        @Bean
        AdminChatProperties adminChatProperties() {
            return new AdminChatProperties();
        }
    }

    @BeforeEach
    void setUp() {
        reviewCaptor = ArgumentCaptor.forClass(AdminDisputeReview.class);
        messageCaptor = ArgumentCaptor.forClass(String.class);
        adminChatProperties.setId(10L);
        var topics = new AdminChatProperties.Topic();
        topics.setReviewDisputesProcessing(20);
        adminChatProperties.setTopics(topics);
    }

    @Test
    @DisplayName("notify handles manual_pending disputes and saves review")
    void notifyManualPendingSendsMessageAndPersistsReview() throws Exception {
        var dispute = new Dispute();
        dispute.setStatus("manual_pending");
        dispute.setInvoiceId(INVOICE_ID);
        dispute.setPaymentId(PAYMENT_ID);
        dispute.setMapping("code = disputes:unknown, description = Unexpected result, code = disputes:Rejected, " +
                "description = test, state = null");

        var providerRef = new ProviderRef().setId(1);
        when(hellgateService.getInvoice(INVOICE_ID)).thenReturn(buildInvoice(providerRef));
        when(dominantService.getProvider(providerRef)).thenReturn(new Provider().setName("Provider"));

        var message = new Message();
        message.setMessageId((int) TG_MESSAGE_ID);
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt()))
                .thenReturn(Optional.of(message));

        notificationHandler.notify(dispute);

        verify(telegramApiService).sendMessage(messageCaptor.capture(),
                eq(adminChatProperties.getId()),
                eq(adminChatProperties.getTopics().getReviewDisputesProcessing()));
        verify(adminDisputeReviewDao).save(reviewCaptor.capture());
        var review = reviewCaptor.getValue();
        assertEquals(INVOICE_ID, review.getInvoiceId());
        assertEquals(PAYMENT_ID, review.getPaymentId());
        assertEquals(TG_MESSAGE_ID, review.getTgMessageId());
        var text = messageCaptor.getValue();
        log.info("Notification text: {}", text);
        var expectedMessage = polyglot.getText("dispute.support.manual-pending-with-text",
                "(1) Provider",
                "invoice-1.payment-1",
                PROVIDER_TRX_ID,
                "mapping:code = disputes:unknown, description = Unexpected result, code = " +
                        "disputes:Rejected, description = test, state = null",
                new StringJoiner("\n"));
        assertEquals(expectedMessage, text);
    }

    @Test
    @DisplayName("notify ignores specific manual_pending")
    void notifyIgnoresSpecificManualPending() throws Exception {
        var dispute = new Dispute();
        dispute.setStatus("manual_pending");
        dispute.setInvoiceId(INVOICE_ID);
        dispute.setPaymentId(PAYMENT_ID);
        dispute.setTechnicalErrorMessage("dispute via disputes-tg-bot");

        notificationHandler.notify(dispute);

        verifyNoInteractions(telegramApiService);
        verifyNoInteractions(adminDisputeReviewDao);
    }

    @Test
    @DisplayName("notify sends manual_pending dispute with attachment")
    void notifyManualPendingSendsMessageWithAttachment() throws Exception {
        byte[] content = "attachment".getBytes();
        var dispute = new Dispute();
        dispute.setDisputeId(DISPUTE_ID.toString());
        dispute.setStatus("manual_pending");
        dispute.setInvoiceId(INVOICE_ID);
        dispute.setPaymentId(PAYMENT_ID);
        dispute.setAttachments(List.of(new Attachment().setData(content)));

        var providerRef = new ProviderRef().setId(1);
        when(hellgateService.getInvoice(INVOICE_ID)).thenReturn(buildInvoice(providerRef));
        when(dominantService.getProvider(providerRef)).thenReturn(new Provider().setName("Provider"));

        var inputFile = new InputFile();
        when(attachmentConverter.convert(eq(DISPUTE_ID), any(Attachment.class))).thenReturn(inputFile);
        var message = new Message();
        message.setMessageId((int) TG_MESSAGE_ID);
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message));

        notificationHandler.notify(dispute);

        verify(attachmentConverter).convert(eq(DISPUTE_ID), any(Attachment.class));
        verify(telegramApiService).sendDocumentReply(
                eq(adminChatProperties.getId()),
                eq(adminChatProperties.getTopics().getReviewDisputesProcessing()),
                eq((int) TG_MESSAGE_ID),
                same(inputFile));
        verify(adminDisputeReviewDao).save(reviewCaptor.capture());
        assertEquals(TG_MESSAGE_ID, reviewCaptor.getValue().getTgMessageId());
        InOrder flow = inOrder(telegramApiService, adminDisputeReviewDao);
        flow.verify(telegramApiService).sendMessage(anyString(), anyLong(), anyInt());
        flow.verify(adminDisputeReviewDao).save(any(AdminDisputeReview.class));
        flow.verify(telegramApiService).sendDocumentReply(anyLong(), anyInt(), anyInt(), same(inputFile));
    }

    @Test
    @DisplayName("notify preserves text flow when attachment preparation fails")
    void notifyManualPendingPreservesTextFlowOnAttachmentFailure() throws Exception {
        var dispute = new Dispute();
        dispute.setDisputeId(DISPUTE_ID.toString());
        dispute.setStatus("manual_pending");
        dispute.setInvoiceId(INVOICE_ID);
        dispute.setPaymentId(PAYMENT_ID);
        dispute.setAttachments(List.of(new Attachment().setData("broken".getBytes())));

        var providerRef = new ProviderRef().setId(1);
        when(hellgateService.getInvoice(INVOICE_ID)).thenReturn(buildInvoice(providerRef));
        when(dominantService.getProvider(providerRef)).thenReturn(new Provider().setName("Provider"));
        var message = new Message();
        message.setMessageId((int) TG_MESSAGE_ID);
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message));
        when(attachmentConverter.convert(eq(DISPUTE_ID), any(Attachment.class)))
                .thenThrow(new IllegalArgumentException("Invalid attachment"));

        notificationHandler.notify(dispute);

        verify(adminDisputeReviewDao).save(reviewCaptor.capture());
        assertEquals(TG_MESSAGE_ID, reviewCaptor.getValue().getTgMessageId());
        verify(telegramApiService, never()).sendDocumentReply(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("notify preserves text flow when dispute id is not UUID")
    void notifyManualPendingPreservesTextFlowOnInvalidDisputeId() throws Exception {
        var dispute = new Dispute();
        dispute.setDisputeId("invalid-dispute-id");
        dispute.setStatus("manual_pending");
        dispute.setInvoiceId(INVOICE_ID);
        dispute.setPaymentId(PAYMENT_ID);
        dispute.setAttachments(List.of(new Attachment().setData("attachment".getBytes())));

        var providerRef = new ProviderRef().setId(1);
        when(hellgateService.getInvoice(INVOICE_ID)).thenReturn(buildInvoice(providerRef));
        when(dominantService.getProvider(providerRef)).thenReturn(new Provider().setName("Provider"));
        var message = new Message();
        message.setMessageId((int) TG_MESSAGE_ID);
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message));

        notificationHandler.notify(dispute);

        verify(adminDisputeReviewDao).save(reviewCaptor.capture());
        assertEquals(TG_MESSAGE_ID, reviewCaptor.getValue().getTgMessageId());
        verifyNoInteractions(attachmentConverter);
        verify(telegramApiService, never()).sendDocumentReply(anyLong(), anyInt(), anyInt(), any());
    }

    private Invoice buildInvoice(ProviderRef providerRef) {
        var invoice = new Invoice();
        var invoiceInner = new dev.vality.damsel.domain.Invoice();
        invoiceInner.setId(INVOICE_ID);
        invoice.setInvoice(invoiceInner);

        var payment = new dev.vality.damsel.domain.InvoicePayment();
        payment.setId(PAYMENT_ID);

        var route = new PaymentRoute();
        route.setProvider(providerRef);
        route.setTerminal(new TerminalRef().setId(2));

        var lastTrxInfo = new TransactionInfo();
        lastTrxInfo.setId(PROVIDER_TRX_ID);

        var processingPayment = new InvoicePayment();
        processingPayment.setPayment(payment);
        processingPayment.setRoute(route);
        processingPayment.setLastTransactionInfo(lastTrxInfo);

        invoice.setPayments(List.of(processingPayment));
        return invoice;
    }
}
