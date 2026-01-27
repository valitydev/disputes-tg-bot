package dev.vality.disputes.tg.bot.servlet.impl;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.domain.Provider;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.TerminalRef;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.admin.Dispute;
import dev.vality.disputes.tg.bot.config.ResponseDictionaryConfig;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.config.properties.DictionaryProperties;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        DisputesApiNotificationHandler.class,
        Polyglot.class,
        ResponseDictionaryConfig.class,
        DisputesApiNotificationHandlerTest.TestConfig.class
})
class DisputesApiNotificationHandlerTest {

    private static final String INVOICE_ID = "invoice-1";
    private static final String PAYMENT_ID = "payment-1";
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
    @Captor
    private ArgumentCaptor<AdminDisputeReview> reviewCaptor;
    @Captor
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
