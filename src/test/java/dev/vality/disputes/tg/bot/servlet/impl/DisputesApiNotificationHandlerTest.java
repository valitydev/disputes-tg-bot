package dev.vality.disputes.tg.bot.servlet.impl;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.domain.Provider;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.TerminalRef;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.admin.AttachmentRef;
import dev.vality.disputes.admin.Dispute;
import dev.vality.disputes.tg.bot.config.ResponseDictionaryConfig;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.config.properties.DictionaryProperties;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.AdminDisputeReview;
import dev.vality.disputes.tg.bot.service.AttachmentService;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final String DISPUTE_ID = "dispute-1";
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
    private AttachmentService attachmentService;
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
    void notifySavesEveryMessageInAlbum() throws Exception {
        var dispute = disputeWithAttachments(2);
        when(telegramApiService.sendMessageWithDocuments(anyString(), anyLong(), anyInt(), anyList()))
                .thenReturn(Optional.of(messages(0, 2)));

        notificationHandler.notify(dispute);

        verify(telegramApiService, never()).sendMessage(anyString(), anyLong(), anyInt());
        verify(attachmentService).prepareAttachment(
                DISPUTE_ID + "-1", dispute.getAttachmentRefs().orElseThrow().get(0));
        verify(attachmentService).prepareAttachment(
                DISPUTE_ID + "-2", dispute.getAttachmentRefs().orElseThrow().get(1));
        assertReviews(2);
    }

    @Test
    void notifySendsSingleAttachmentWithCaption() throws Exception {
        var dispute = disputeWithAttachments(1);
        when(telegramApiService.sendMessageWithDocument(anyString(), anyLong(), anyInt(), any()))
                .thenReturn(Optional.of(message(0)));

        notificationHandler.notify(dispute);

        verify(telegramApiService).sendMessageWithDocument(anyString(), eq(adminChatProperties.getId()),
                eq(adminChatProperties.getTopics().getReviewDisputesProcessing()), any());
        verify(telegramApiService, never()).sendMessage(anyString(), anyLong(), anyInt());
        verify(telegramApiService, never()).sendMessageWithDocuments(any(), anyLong(), anyInt(), anyList());
        assertReviews(1);
    }

    @Test
    void notifySkipsBrokenAttachment() throws Exception {
        var dispute = disputeWithAttachments(2);
        when(attachmentService.prepareAttachment(eq(DISPUTE_ID + "-1"), any()))
                .thenThrow(new IllegalArgumentException("Invalid attachment"));
        when(telegramApiService.sendMessageWithDocument(anyString(), anyLong(), anyInt(), any()))
                .thenReturn(Optional.of(message(0)));

        notificationHandler.notify(dispute);

        verify(telegramApiService).sendMessageWithDocument(anyString(), anyLong(), anyInt(), any());
        verify(telegramApiService, never()).sendMessageWithDocuments(any(), anyLong(), anyInt(), anyList());
        assertReviews(1);
    }

    @Test
    void notifySendsTextWhenAllAttachmentsFailPreparation() throws Exception {
        var dispute = disputeWithAttachments(2);
        when(attachmentService.prepareAttachment(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid attachment"));
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message(0)));

        notificationHandler.notify(dispute);

        verify(telegramApiService).sendMessage(anyString(), anyLong(), anyInt());
        verify(telegramApiService, never()).sendMessageWithDocument(any(), anyLong(), anyInt(), any());
        verify(telegramApiService, never()).sendMessageWithDocuments(any(), anyLong(), anyInt(), anyList());
        assertReviews(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 11, 20, 21})
    void notifySendsOnlyFirstTenAttachments(int count) throws Exception {
        var dispute = disputeWithAttachments(count);
        when(telegramApiService.sendMessageWithDocuments(anyString(), anyLong(), anyInt(), anyList()))
                .thenReturn(Optional.of(messages(0, 10)));

        notificationHandler.notify(dispute);

        verify(attachmentService, times(10)).prepareAttachment(anyString(), any());
        verify(telegramApiService).sendMessageWithDocuments(anyString(), anyLong(), anyInt(),
                argThat(files -> files.stream().map(InputFile::getMediaName).toList()
                        .equals(IntStream.range(0, 10).mapToObj(Integer::toString).toList())));
        verify(telegramApiService, never()).sendMessageWithDocument(any(), anyLong(), anyInt(), any());
        verify(telegramApiService, never()).sendMessage(anyString(), anyLong(), anyInt());
        assertReviews(10);
    }

    @Test
    void notifyDoesNotReplaceFailedAttachmentWithEleventh() throws Exception {
        var dispute = disputeWithAttachments(11);
        when(attachmentService.prepareAttachment(eq(DISPUTE_ID + "-1"), any()))
                .thenThrow(new IllegalArgumentException("Invalid attachment"));
        when(telegramApiService.sendMessageWithDocuments(anyString(), anyLong(), anyInt(), anyList()))
                .thenReturn(Optional.of(messages(0, 9)));

        notificationHandler.notify(dispute);

        verify(attachmentService, times(10)).prepareAttachment(anyString(), any());
        verify(attachmentService, never()).prepareAttachment(eq(DISPUTE_ID + "-11"), any());
        verify(telegramApiService).sendMessageWithDocuments(anyString(), anyLong(), anyInt(),
                argThat(files -> files.size() == 9));
        assertReviews(9);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void notifySendsLongTextSeparatelyBeforeAttachments(int count) throws Exception {
        final var dispute = disputeWithAttachments(count).setProviderMessage("x".repeat(1025));
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message(0)));
        when(telegramApiService.sendMessageWithDocument(isNull(), anyLong(), anyInt(), any()))
                .thenReturn(Optional.of(message(1)));
        when(telegramApiService.sendMessageWithDocuments(isNull(), anyLong(), anyInt(), anyList()))
                .thenReturn(Optional.of(messages(1, count)));

        notificationHandler.notify(dispute);

        var order = inOrder(telegramApiService);
        order.verify(telegramApiService).sendMessage(messageCaptor.capture(), anyLong(), anyInt());
        assertTrue(messageCaptor.getValue().contains("x".repeat(1025)));
        if (count == 1) {
            order.verify(telegramApiService).sendMessageWithDocument(isNull(), anyLong(), anyInt(), any());
        } else {
            order.verify(telegramApiService).sendMessageWithDocuments(isNull(), anyLong(), anyInt(), anyList());
        }
        assertReviews(count + 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 20})
    void notifyFallsBackToTextOnceWhenSendingAttachmentsFails(int count) throws Exception {
        var dispute = disputeWithAttachments(count);
        when(telegramApiService.sendMessage(anyString(), anyLong(), anyInt())).thenReturn(Optional.of(message(0)));

        notificationHandler.notify(dispute);

        verify(telegramApiService).sendMessage(anyString(), anyLong(), anyInt());
        assertReviews(1);
    }

    @Test
    void notifyFailsWhenTextCannotBeDeliveredEither() throws Exception {
        var dispute = disputeWithAttachments(2);

        assertThrows(java.util.NoSuchElementException.class, () -> notificationHandler.notify(dispute));

        verifyNoInteractions(adminDisputeReviewDao);
    }

    private Dispute disputeWithAttachments(int count) throws Exception {
        var dispute = new Dispute().setDisputeId(DISPUTE_ID).setStatus("manual_pending")
                .setInvoiceId(INVOICE_ID).setPaymentId(PAYMENT_ID);
        var attachments = IntStream.range(0, count)
                .mapToObj(i -> new AttachmentRef("https://storage.example/" + i, "application/pdf")).toList();
        dispute.setAttachmentRefs(attachments);
        for (int i = 0; i < count; i++) {
            var file = new InputFile(new ByteArrayInputStream(new byte[0]), Integer.toString(i));
            when(attachmentService.prepareAttachment(DISPUTE_ID + "-" + (i + 1), attachments.get(i)))
                    .thenReturn(file);
        }
        var providerRef = new ProviderRef().setId(1);
        when(hellgateService.getInvoice(INVOICE_ID)).thenReturn(buildInvoice(providerRef));
        when(dominantService.getProvider(providerRef)).thenReturn(new Provider().setName("Provider"));
        return dispute;
    }

    private List<Message> messages(int start, int count) {
        return IntStream.range(start, start + count).mapToObj(this::message).toList();
    }

    private Message message(int index) {
        var message = new Message();
        message.setMessageId((int) TG_MESSAGE_ID + index);
        return message;
    }

    private void assertReviews(int count) {
        verify(adminDisputeReviewDao, times(count)).save(reviewCaptor.capture());
        assertEquals(IntStream.range(0, count).mapToObj(i -> TG_MESSAGE_ID + i).toList(),
                reviewCaptor.getAllValues().stream().map(AdminDisputeReview::getTgMessageId).toList());
        for (var review : reviewCaptor.getAllValues()) {
            assertEquals(INVOICE_ID, review.getInvoiceId());
            assertEquals(PAYMENT_ID, review.getPaymentId());
        }
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
