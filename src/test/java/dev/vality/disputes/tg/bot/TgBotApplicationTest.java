package dev.vality.disputes.tg.bot;

import dev.vality.bender.BenderSrv;
import dev.vality.bender.GetInternalIDResult;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.disputes.admin.ManualParsingServiceSrv;
import dev.vality.disputes.merchant.*;
import dev.vality.disputes.provider.TransactionContext;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.provider.service.DisputesHandler;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class TgBotApplicationTest {

    @Autowired
    DisputesHandler disputesHandler;

    @MockBean
    protected InvoicingSrv.Iface invoicingSrv;

    @MockBean
    protected MerchantDisputesServiceSrv.Iface disputesMerchantClient;

    @MockBean
    protected ManualParsingServiceSrv.Iface manualParsingClient;

    @MockBean
    protected BenderSrv.Iface benderSrv;

    @MockBean
    protected DisputesBot disputesBot;

    @Test
    public void contextLoads() throws InterruptedException, TException {
        when(invoicingSrv.get(any(), any())).thenReturn(buildInvoice());
        when(disputesMerchantClient.createDispute(any())).thenReturn(buildDisputeCreatedResult());
        when(disputesMerchantClient.checkDisputeStatus(any())).thenReturn(buildDisputeStatusApprovedResult());
        when(benderSrv.getInternalID(any())).thenReturn(new GetInternalIDResult().setInternalId("12345678903"));
    }

    @Test
    public void createProvider() throws InterruptedException, TException {
    }

    private Invoice buildInvoice() {
        var domainInvoice = new dev.vality.damsel.domain.Invoice();
        domainInvoice.setExternalId(UUID.randomUUID().toString());
        domainInvoice.setId(UUID.randomUUID().toString());

        Invoice invoice = new Invoice();
        invoice.setInvoice(domainInvoice);

        InvoicePayment payment = new InvoicePayment();
        payment.setPayment(new dev.vality.damsel.domain.InvoicePayment().setId(UUID.randomUUID().toString()));
        invoice.setPayments(List.of(payment));
        return invoice;
    }

    private DisputeCreatedResult buildDisputeCreatedResult() {
        DisputeCreatedResult disputeCreatedResult = new DisputeCreatedResult();
        disputeCreatedResult.setSuccessResult(new DisputeCreatedSuccessResult(UUID.randomUUID().toString()));
        return disputeCreatedResult;
    }

    private DisputeStatusResult buildDisputeStatusPendingResult() {
        DisputeStatusResult disputeStatusResult = new DisputeStatusResult();
        disputeStatusResult.setStatusPending(new DisputeStatusPendingResult());
        return disputeStatusResult;
    }

    private DisputeStatusResult buildDisputeStatusApprovedResult() {
        DisputeStatusResult disputeStatusResult = new DisputeStatusResult();
        disputeStatusResult.setStatusSuccess(new DisputeStatusSuccessResult());
        return disputeStatusResult;
    }

}