package dev.vality.disputes.tg.bot.handler.admin;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface AdminMessageHandler {

    boolean filter(final Update message);

    void handle(Update message);
}
