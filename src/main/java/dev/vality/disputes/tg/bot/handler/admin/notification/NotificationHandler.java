package dev.vality.disputes.tg.bot.handler.admin.notification;

public interface NotificationHandler<T> {

    void handle(T notification);
}
