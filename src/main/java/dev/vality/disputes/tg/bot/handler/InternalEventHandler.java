package dev.vality.disputes.tg.bot.handler;

public interface InternalEventHandler<T> {

    void handle(T event);
}
