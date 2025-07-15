package dev.vality.disputes.tg.bot.dto.command;

import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class BaseCommand {

    private CommandValidationError validationError;

    public boolean hasValidationError() {
        return validationError != null;
    }
} 