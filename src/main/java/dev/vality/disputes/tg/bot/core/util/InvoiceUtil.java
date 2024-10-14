package dev.vality.disputes.tg.bot.core.util;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.tg.bot.core.exception.NotFoundException;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class InvoiceUtil {

    public static InvoicePayment getInvoicePayment(Invoice invoice, String paymentId) {
        return invoice.getPayments().stream()
                .filter(p -> paymentId.equals(p.getPayment().getId()) && p.isSetRoute())
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Payment with id: %s and filled route not found!", paymentId)));
    }

    public static String getProviderTrxId(InvoicePayment payment) {
        return Optional.ofNullable(payment.getLastTransactionInfo())
                .map(TransactionInfo::getId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Payment with id: %s and filled ProviderTrxId not found!",
                                payment.getPayment().getId())));
    }

    public static ProviderRef getProviderRef(Invoice invoice, String paymentId) {
        return invoice.getPayments().stream()
                .filter(p -> paymentId.equals(p.getPayment().getId()) && p.isSetRoute())
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Invoice %s and Payment %s with filled route not found!",
                                invoice.getInvoice().getId(),
                                paymentId)))
                .getRoute().getProvider();
    }
}
