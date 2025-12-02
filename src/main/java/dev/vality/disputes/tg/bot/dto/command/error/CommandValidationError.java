package dev.vality.disputes.tg.bot.dto.command.error;

import lombok.Getter;

@Getter
public enum CommandValidationError {

    ARGUMENT_NUMBER_MISMATCH("error.input.invalid-command-format"),

    INVALID_CHAT_ID("error.input.invalid-chat-id"),

    CHAT_NOT_FOUND_IN_TELEGRAM("error.chat.not-found"),

    CHAT_NOT_FOUND_IN_DATABASE("error.chat.not-found"),

    INVALID_PROVIDER_ID("error.input.invalid-provider-id"),

    INVALID_TERMINAL_ID("error.input.invalid-terminal-id"),

    INVALID_EXTERNAL_ID("error.input.invalid-external-id"),

    INVALID_NUMBER_FORMAT("error.input.invalid-number-format"),

    INVALID_COMMAND_FORMAT("error.input.invalid-command-format");

    private final String messageKey;
    
    CommandValidationError(String messageKey) {
        this.messageKey = messageKey;
    }

} 