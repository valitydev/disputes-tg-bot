package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class Polyglot {

    private static final Locale DEFAULT_LOCALE = Locale.of("ru");
    private final MessageSource messageSource;

    public Locale getLocale() {
        return getLocale(null);
    }

    public Locale getLocale(String defaultLocale) {
        // Default locale has higher priority
        if (defaultLocale != null) {
            return Locale.of(defaultLocale);
        }
        return DEFAULT_LOCALE;
    }

    public String getText(Locale locale, String phraseKey, Object... args) {
        return messageSource.getMessage(phraseKey, escapeHtmlArgs(args), locale);
    }

    public String getText(String phraseKey, Object... args) {
        return getText(DEFAULT_LOCALE, phraseKey, args);
    }

    private Object[] escapeHtmlArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        return Arrays.stream(args)
                .map(arg -> arg == null ? null : TelegramUtil.escapeHtml(arg.toString()))
                .toArray(Object[]::new);
    }
}
