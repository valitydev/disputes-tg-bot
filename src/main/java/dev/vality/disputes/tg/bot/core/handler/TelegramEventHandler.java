package dev.vality.disputes.tg.bot.core.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramEventHandler {

    boolean filter(final Update message);

    void handle(Update message);
}
