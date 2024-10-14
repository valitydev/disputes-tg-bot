package dev.vality.disputes.tg.bot.common.util;

import dev.vality.disputes.tg.bot.common.dto.DisputeInfoDto;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextParsingUtil {

    public static final String INVOICE_ID_REGEX = "[a-zA-Z0-9]{11}(?:\\.\\d+)?";
    public static final Pattern INVOICE_ID_REGEX_PATTERN = Pattern.compile(INVOICE_ID_REGEX);

    public static final String DISPUTE_ID_REGEX =
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    public static final Pattern DISPUTE_ID_REGEX_PATTERN = Pattern.compile(DISPUTE_ID_REGEX);

    public static final String PAYMENT_ID_REGEX = "\\.\\d+";
    public static final Pattern PAYMENT_ID_REGEX_PATTERN = Pattern.compile(PAYMENT_ID_REGEX);

    public static Optional<DisputeInfoDto> getDisputeInfo(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = text.split("\\s");
        DisputeInfoDto.DisputeInfoDtoBuilder disputeInfoBuilder = DisputeInfoDto.builder();
        for (String part : parts) {
            if (INVOICE_ID_REGEX_PATTERN.matcher(part).matches()) {
                String invoiceId = removePaymentId(part);
                String paymentId = getPaymentId(part);
                disputeInfoBuilder.invoiceId(invoiceId)
                        .paymentId(paymentId);
            }

            if (DISPUTE_ID_REGEX_PATTERN.matcher(part).matches()) {
                disputeInfoBuilder.disputeId(part);
            }
        }
        DisputeInfoDto disputeInfo = disputeInfoBuilder.build();
        if (disputeInfo.getInvoiceId() == null && disputeInfo.getDisputeId() == null) {
            return Optional.empty();
        }
        return Optional.of(disputeInfo);
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
}
