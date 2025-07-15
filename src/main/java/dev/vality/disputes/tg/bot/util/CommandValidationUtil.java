package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.text.html.Option;
import java.util.Optional;

@Slf4j
@UtilityClass
public class CommandValidationUtil {

    private static final String nullValue = "null";

    public static Optional<Integer> extractInteger(String value, String parameterName) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            log.warn("Unable to parse int type from value '{}' for parameter with name '{}'", value, parameterName);
            return Optional.empty();
        }
    }

    public static Optional<Long> extractLong(String value, String parameterName) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            log.warn("Unable to parse long type from value '{}' for parameter with name '{}'", value, parameterName);
            return Optional.empty();
        }
    }

    public static String extractNullableString(String value) {
        if (isNull(value)) {
            return null;
        }
        return value;
    }

    public static String[] extractArgs(String messageText) {
        String[] parts = messageText.split("\\s+");
        if (parts.length < 2) {
            return new String[0];
        }

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return args;
    }

    public static boolean hasExpectedArgsCount(String messageText, int expectedCount) {
        String[] parts = messageText.split("\\s+");
        if (parts.length < expectedCount + 1) {
            log.warn("Invalid command format: expected {} arguments, got {}", expectedCount, parts.length - 1);
            return false;
        }
        return true;
    }

    public static boolean isNull(String value) {
        return nullValue.equals(value);
    }
}
