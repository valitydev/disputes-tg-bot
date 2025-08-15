package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextParsingUtil {

    public static final String INVOICE_PAYMENT_ID_REGEX = "[a-zA-Z0-9]{11}\\.\\d+";
    public static final Pattern INVOICE_PAYMENT_ID_REGEX_PATTERN = Pattern.compile(INVOICE_PAYMENT_ID_REGEX);
    public static final String INVOICE_ID_ONLY_REGEX = "[a-zA-Z0-9]{11}(?!\\.\\d)";
    public static final Pattern INVOICE_ID_ONLY_REGEX_PATTERN = Pattern.compile(INVOICE_ID_ONLY_REGEX);

    public static final String INVOICE_ID_AND_BEFORE_INCLUSIVE_REGEX = "^.*[a-zA-Z0-9]{11}(?:\\.\\d+)?";

    public static final String DISPUTE_ID_REGEX =
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    public static final Pattern DISPUTE_ID_REGEX_PATTERN = Pattern.compile(DISPUTE_ID_REGEX);

    public static final String PAYMENT_ID_REGEX = "\\.\\d+";
    public static final Pattern PAYMENT_ID_REGEX_PATTERN = Pattern.compile(PAYMENT_ID_REGEX);

    public static Optional<DisputeInfoDto> getDisputeInfo(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        var builder = DisputeInfoDto.builder();
        var invoiceMatcher = INVOICE_ID_ONLY_REGEX_PATTERN.matcher(text);
        if (invoiceMatcher.find()) {
            String invoiceWithPayment = invoiceMatcher.group();
            builder.invoiceId(removePaymentId(invoiceWithPayment));
        }
        var invoicePaymentMatcher = INVOICE_PAYMENT_ID_REGEX_PATTERN.matcher(text);
        if (invoicePaymentMatcher.find()) {
            String invoiceWithPayment = invoicePaymentMatcher.group();
            builder.invoiceId(removePaymentId(invoiceWithPayment))
                    .paymentId(getPaymentId(invoiceWithPayment));
        }
        var disputeMatcher = DISPUTE_ID_REGEX_PATTERN.matcher(text);
        if (disputeMatcher.find()) {
            builder.disputeId(disputeMatcher.group());
        }
        var result = builder.build();
        if (result.getInvoiceId() == null && result.getDisputeId() == null) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    public static List<DisputeInfoDto> getDeclineDisputeInfos(String text) {
        return getLinesWithDisputes(text).stream()
                .map(TextParsingUtil::extractDeclinedDisputeInfo)
                .filter(disputeInfoDto -> disputeInfoDto.getInvoiceId() != null
                        || disputeInfoDto.getDisputeId() != null).toList();
    }

    private DisputeInfoDto extractDeclinedDisputeInfo(String line) {
        var builder = DisputeInfoDto.builder();
        var invoiceMatcher = INVOICE_ID_ONLY_REGEX_PATTERN.matcher(line);
        if (invoiceMatcher.find()) {
            String invoiceWithPayment = invoiceMatcher.group();
            builder.invoiceId(removePaymentId(invoiceWithPayment));
        }
        var invoicePaymentMatcher = INVOICE_PAYMENT_ID_REGEX_PATTERN.matcher(line);
        if (invoicePaymentMatcher.find()) {
            String invoiceWithPayment = invoicePaymentMatcher.group();
            builder.invoiceId(removePaymentId(invoiceWithPayment))
                    .paymentId(getPaymentId(invoiceWithPayment));
        }
        String errorText = line.replaceFirst(INVOICE_ID_AND_BEFORE_INCLUSIVE_REGEX, "").trim();
        builder.responseText(errorText);
        return builder.build();
    }

    private List<String> getLinesWithDisputes(String text) {
        return Arrays.stream(text.split("\\n"))
                .filter(
                        line -> line.matches(".*" + DISPUTE_ID_REGEX_PATTERN + ".*")
                                || line.matches(".*" + INVOICE_PAYMENT_ID_REGEX_PATTERN + ".*"))
                .toList();
    }

    private static String removePaymentId(String text) {
        return PAYMENT_ID_REGEX_PATTERN.matcher(text).replaceAll("");
    }

    private static String getPaymentId(String text) {
        Matcher matcher = PAYMENT_ID_REGEX_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        // Strip dot at the beginning
        return matcher.group(0).substring(1);
    }

    public static Optional<String> getExternalId(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String[] parts = text.split("\\s");
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(parts[1]);
    }

    public static boolean hasInvoiceId(String text) {
        return INVOICE_PAYMENT_ID_REGEX_PATTERN.matcher(text).hasMatch();
    }
}
