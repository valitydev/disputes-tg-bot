package dev.vality.disputes.tg.bot.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

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
        return messageSource.getMessage(phraseKey, args, locale);
    }

    public String getText(String phraseKey, Object... args) {
        return getText(DEFAULT_LOCALE, phraseKey, args);
    }
}
