package dev.vality.disputes.tg.bot.common.service;

import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
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

    public String getLocale(Update update, Chat chat) {
        if (chat != null && chat.getLocale() != null) {
            return chat.getLocale();
        }
        if (update.hasMessage() && update.getMessage().getFrom().getLanguageCode() != null) {
            return update.getMessage().getFrom().getLanguageCode();
        }
        return DEFAULT_LOCALE.getLanguage();
    }

    public String getText(String localeCode, String phraseKey, Object... args) {
        Locale locale = localeCode == null ? DEFAULT_LOCALE : Locale.of(localeCode);
        return messageSource.getMessage(phraseKey, args, locale);
    }
}
