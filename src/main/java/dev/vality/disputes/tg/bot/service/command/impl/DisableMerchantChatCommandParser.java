package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.DisableMerchantChatCommand;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisableMerchantChatCommandParser implements CommandParser<DisableMerchantChatCommand> {

    private static final String LATIN_SINGLE_LETTER = "/dm ";
    private static final String FULL_COMMAND = "/disable_merch ";

    @Override
    public DisableMerchantChatCommand parse(@NonNull String messageText) {
        if (!CommandValidationUtil.hasExpectedArgsCount(messageText, 1)) {
            return DisableMerchantChatCommand.builder()
                    .validationError(CommandValidationError.ARGUMENT_NUMBER_MISMATCH)
                    .build();
        }

        String[] args = CommandValidationUtil.extractArgs(messageText);
        var chatId = CommandValidationUtil.extractLong(args[0], "Chat ID");
        if (chatId.isEmpty()) {
            return DisableMerchantChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_CHAT_ID)
                    .build();
        }

        return DisableMerchantChatCommand.builder()
                .chatId(chatId.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 