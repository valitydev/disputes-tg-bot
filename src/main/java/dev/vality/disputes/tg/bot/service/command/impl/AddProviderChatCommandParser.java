package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.AddProviderChatCommand;
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
public class AddProviderChatCommandParser implements CommandParser<AddProviderChatCommand> {

    private static final String LATIN_SINGLE_LETTER = "/ap ";
    private static final String FULL_COMMAND = "/add_provider ";

    @Override
    public AddProviderChatCommand parse(@NonNull String messageText) {
        if (!CommandValidationUtil.hasExpectedArgsCount(messageText, 5)) {
            return AddProviderChatCommand.builder()
                    .validationError(CommandValidationError.ARGUMENT_NUMBER_MISMATCH)
                    .build();
        }

        String[] args = CommandValidationUtil.extractArgs(messageText);

        var providerId = CommandValidationUtil.extractInteger(args[0], "Provider ID");
        if (providerId.isEmpty()) {
            return AddProviderChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_PROVIDER_ID)
                    .build();
        }

        var sendToChatId = CommandValidationUtil.extractLong(args[1], "Send to chat ID");
        if (sendToChatId.isEmpty()) {
            return AddProviderChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_CHAT_ID)
                    .build();
        }

        Integer sendToTopicId = null;
        if (!CommandValidationUtil.isNull(args[2])) {
            var sendToTopicIdOpt = CommandValidationUtil.extractInteger(args[2], "Send to topic ID");
            if (sendToTopicIdOpt.isEmpty()) {
                return AddProviderChatCommand.builder()
                        .validationError(CommandValidationError.INVALID_NUMBER_FORMAT)
                        .build();
            }
            sendToTopicId = sendToTopicIdOpt.get();
        }

        var readFromChatId = CommandValidationUtil.extractLong(args[3], "Read from chat ID");
        if (readFromChatId.isEmpty()) {
            return AddProviderChatCommand.builder()
                    .validationError(CommandValidationError.INVALID_CHAT_ID)
                    .build();
        }

        var template = CommandValidationUtil.extractNullableString(args[4]);

        return AddProviderChatCommand.builder()
                .providerId(providerId.get())
                .sendToChatId(sendToChatId.get())
                .sendToTopicId(sendToTopicId)
                .readFromChatId(readFromChatId.get())
                .template(template)
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 