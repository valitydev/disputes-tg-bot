package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandValidationUtil {

    private static final String nullValue = "null";

    public static Integer extractInteger(String value, String parameterName) throws CommandValidationException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandValidationException(
                    "'%s' must be integer, but got '%s'".formatted(parameterName, value));
        }
    }

    public static Long extractLong(String value, String parameterName) throws CommandValidationException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CommandValidationException(
                    "'%s' must be long, but got '%s'".formatted(parameterName, value));
        }
    }

    public static Integer extractNullableInteger(String value, String parameterName) throws CommandValidationException {
        if (nullValue.equals(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandValidationException(
                    "'%s' must be integer, but got '%s'".formatted(parameterName, value));
        }
    }

    public static String extractNullableString(String value) {
        if (nullValue.equals(value)) {
            return null;
        }
        return value;
    }
}
