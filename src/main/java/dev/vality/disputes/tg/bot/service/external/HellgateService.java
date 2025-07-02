package dev.vality.disputes.tg.bot.service.external;

import dev.vality.damsel.payment_processing.EventRange;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.disputes.tg.bot.exception.HellgateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HellgateService {

    private final InvoicingSrv.Iface invoicingClient;

    public Invoice getInvoice(String invoiceId) throws InvoiceNotFound {
        try {
            log.debug("Looking for invoice with id: {}", invoiceId);
            Invoice invoice = invoicingClient.get(invoiceId, new EventRange());
            log.debug("Found invoice with id: {} and {} payments", invoiceId, invoice.getPayments().size());
            return invoice;
        } catch (InvoiceNotFound e) {
            log.error("Unable to find invoice with id: {}}", invoiceId, e);
            throw e;
        } catch (Exception e2) {
            throw new HellgateException(String.format("Failed to get invoice with id: %s", invoiceId), e2);
        }
    }
}
