package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.provider.TransactionContext;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@UtilityClass
public class FormatUtil {

    private static final String MASKED_VALUE = "masked";
    private static final Pattern IBAN_PATTERN = Pattern.compile(
            "(?i)(?<![a-z0-9])[a-z]{2}\\d{2}(?:[-\\h]?[a-z0-9]){11,30}(?![a-z0-9])");
    private static final Pattern REQUISITES_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\d[-\\h]?){11,}\\d(?!\\d)");

    public static String prepareForLog(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        var maskedSource = IBAN_PATTERN.matcher(source).replaceAll(MASKED_VALUE);
        return REQUISITES_PATTERN.matcher(maskedSource).replaceAll(MASKED_VALUE);
    }

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
        return new BigDecimal(cash.getAmount())
                .movePointLeft(cash.getCurrency().getExponent())
                .stripTrailingZeros()
                .toPlainString();
    }

    public static String getFormattedAmount(Long changedAmount) {
        return new BigDecimal(changedAmount)
                .movePointLeft(2)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static String formatOptional(String value) {
        return value == null ? "null" : value;
    }
}
