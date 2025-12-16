package dev.vality.disputes.tg.bot.service.command;

import org.springframework.lang.NonNull;

public interface CommandParser<T> {

    T parse(@NonNull String commandString);

    boolean canParse(@NonNull String commandString);
} 