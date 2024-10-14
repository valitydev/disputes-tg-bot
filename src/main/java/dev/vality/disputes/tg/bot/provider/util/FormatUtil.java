package dev.vality.disputes.tg.bot.provider.util;

import dev.vality.disputes.provider.TransactionContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FormatUtil {

    public static String formatPaymentId(TransactionContext transactionContext) {
        return formatPaymentId(transactionContext.getInvoiceId(), transactionContext.getPaymentId());
    }

    public static String formatPaymentId(String invoiceId, String paymentID) {
        return String.format("%s.%s", invoiceId, paymentID);
    }
}
