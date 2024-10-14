package dev.vality.disputes.tg.bot.support.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface SupportMessageHandler {

    boolean filter(final Update message);

    void handle(Update message);
}
