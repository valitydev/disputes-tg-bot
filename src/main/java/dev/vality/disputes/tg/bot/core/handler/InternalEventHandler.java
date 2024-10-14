package dev.vality.disputes.tg.bot.core.handler;

public interface InternalEventHandler<T> {

    void handle(T event);
}
