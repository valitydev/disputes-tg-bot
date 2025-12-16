package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.AddMerchantChatCommand;
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
public class AddMerchantChatCommandParser implements CommandParser<AddMerchantChatCommand> {

    private static final String LATIN_SINGLE_LETTER = "/am ";
    private static final String FULL_COMMAND = "/add_merch ";

    @Override
    public AddMerchantChatCommand parse(@NonNull String messageText) {
        if (!CommandValidationUtil.hasExpectedArgsCount(messageText, 1)) {
            return AddMerchantChatCommand.builder()
                    .validationError(CommandValidationError.ARGUMENT_NUMBER_MISMATCH)
                    .build();
        }

        String[] args = CommandValidationUtil.extractArgs(messageText);
        var chatId = CommandValidationUtil.extractLong(args[0], "Chat ID");
        if (chatId.isEmpty()) {
            return AddMerchantChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_CHAT_ID)
                    .build();
        }

        return AddMerchantChatCommand.builder()
                .chatId(chatId.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 