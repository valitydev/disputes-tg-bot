package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.DisableProviderChatCommand;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisableProviderChatCommandParser implements CommandParser<DisableProviderChatCommand> {

    private static final String LATIN_SINGLE_LETTER = "/dp ";
    private static final String FULL_COMMAND = "/disable_provider ";

    @Override
    public DisableProviderChatCommand parse(@NonNull String messageText) {
        if (!CommandValidationUtil.hasExpectedArgsCount(messageText, 1)) {
            return DisableProviderChatCommand.builder()
                    .validationError(CommandValidationError.ARGUMENT_NUMBER_MISMATCH)
                    .build();
        }

        String[] args = CommandValidationUtil.extractArgs(messageText);
        
        var providerId = CommandValidationUtil.extractInteger(args[0], "Provider ID");
        if (providerId.isEmpty()) {
            return DisableProviderChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_PROVIDER_ID)
                    .build();
        }

        return DisableProviderChatCommand.builder()
                .providerId(providerId.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 