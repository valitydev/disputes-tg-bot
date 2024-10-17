package dev.vality.disputes.tg.bot.provider.util;

import dev.vality.disputes.tg.bot.provider.constant.TerminalOption;
import dev.vality.disputes.tg.bot.provider.exception.TerminalConfigurationException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@UtilityClass
public class OptionsUtil {

    public static long getChatId(Map<String, String> terminalOptions) {
        if (!terminalOptions.containsKey(TerminalOption.PROVIDER_TELEGRAM_CHAT_ID)) {
            log.error("Unable to find mandatory '{}' terminal setting", TerminalOption.PROVIDER_TELEGRAM_CHAT_ID);
            throw new TerminalConfigurationException(
                    "Unable to find mandatory setting '%s'".formatted(TerminalOption.PROVIDER_TELEGRAM_CHAT_ID));
        }
        var value = terminalOptions.get(TerminalOption.PROVIDER_TELEGRAM_CHAT_ID);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Setting '{}' has value {} and it is not convertable to long",
                    TerminalOption.PROVIDER_TELEGRAM_CHAT_ID, value);
            throw new TerminalConfigurationException(
                    "Setting '%s' is not convertable to long".formatted(TerminalOption.PROVIDER_TELEGRAM_CHAT_ID));
        }
    }
}
