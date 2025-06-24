package dev.vality.disputes.tg.bot.core.util;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.provider.TransactionContext;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class FormatUtil {

    public static String formatPaymentId(TransactionContext transactionContext) {
        return formatPaymentId(transactionContext.getInvoiceId(), transactionContext.getPaymentId());
    }

    public static String formatPaymentId(String invoiceId, String paymentID) {
        return String.format("%s.%s", invoiceId, paymentID);
    }

    public static String getFormattedAmount(DisputeParams disputeParams) {
        if (disputeParams.getCash().isEmpty()) {
            return "null";
        }

        var cash = disputeParams.getCash().get();
        var formattedAmount =
                new BigDecimal(cash.getAmount())
                        .movePointLeft(cash.getCurrency().getExponent())
                        .stripTrailingZeros()
                        .toPlainString();
        return "%s %s".formatted(formattedAmount, cash.getCurrency().getSymbolicCode());
    }
} 