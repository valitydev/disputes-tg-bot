package dev.vality.disputes.tg.bot.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class Polyglot {

    private static final Locale DEFAULT_LOCALE = Locale.of("ru");
    private final MessageSource messageSource;

    public String getLocale(Update update) {
        return getLocale(update, null);
    }

    public String getLocale(Update update, String defaultLocale) {
        // Default locale has higher priority
        if (defaultLocale != null) {
            return defaultLocale;
        }
        if (update.hasMessage() && update.getMessage().getFrom().getLanguageCode() != null) {
            return update.getMessage().getFrom().getLanguageCode();
        }
        return DEFAULT_LOCALE.getLanguage();
    }

    public String getText(Locale locale, String phraseKey, Object... args) {
        return messageSource.getMessage(phraseKey, args, locale);
    }

    public String getText(String phraseKey, Object... args) {
        return getText(DEFAULT_LOCALE, phraseKey, args);
    }
}
