package dev.vality.alert.tg.bot.handler;

import org.apache.thrift.TException;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommonHandler<T> {

    boolean filter(final Update update);

    T handle(Update update, long userId) throws TException;

}
