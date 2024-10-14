package dev.vality.disputes.tg.bot.support.handler.notification;

public interface NotificationHandler<T> {

    void handle(T notification);
}
