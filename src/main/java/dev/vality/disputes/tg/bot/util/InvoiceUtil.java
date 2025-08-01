package dev.vality.disputes.tg.bot.util;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.TerminalRef;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.disputes.tg.bot.exception.NotFoundException;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Optional;

@UtilityClass
public class InvoiceUtil {

    private static final String[] targetKeywords = new String[]{"target", "masked"};


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
        return getRoute(invoice, paymentId).getProvider();
    }

    public static PaymentRoute getRoute(Invoice invoice, String paymentId) {
        return invoice.getPayments().stream()
                .filter(p -> paymentId.equals(p.getPayment().getId()) && p.isSetRoute())
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Invoice %s and Payment %s with filled route not found!",
                                invoice.getInvoice().getId(),
                                paymentId)))
                .getRoute();
    }

    public static String getTarget(Invoice invoice, String paymentId) {
        var extraEntry = getInvoicePayment(invoice, paymentId).getLastTransactionInfo()
                .getAdditionalInfo().getExtraPaymentInfo().entrySet().stream()
                .filter(entry -> {
                    var key = entry.getKey();
                    return Arrays.stream(targetKeywords).anyMatch(key::contains);
                }).findFirst();
        return extraEntry.map(entry -> "%s: %s".formatted(entry.getKey(), entry.getValue())).orElse(null);
    }

    public static TerminalRef getTerminalRef(Invoice invoice, String paymentId) {
        return getRoute(invoice, paymentId).getTerminal();
    }
}
