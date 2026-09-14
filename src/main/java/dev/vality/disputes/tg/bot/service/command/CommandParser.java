package dev.vality.disputes.tg.bot.service.command;

import org.jspecify.annotations.NonNull;

public interface CommandParser<T> {

    T parse(@NonNull String commandString);

    boolean canParse(@NonNull String commandString);
} 