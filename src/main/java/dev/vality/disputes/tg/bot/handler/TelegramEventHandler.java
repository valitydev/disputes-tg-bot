package dev.vality.disputes.tg.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface TelegramEventHandler {

    boolean filter(final Update message);

    void handle(Update message);

    default void handle(List<Update> messages) {
        messages.forEach(this::handle);
    }
}
