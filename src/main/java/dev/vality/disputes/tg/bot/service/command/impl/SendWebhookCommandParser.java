package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.command.SendWebhookCommand;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendWebhookCommandParser implements CommandParser<SendWebhookCommand> {

    private static final String LATIN_SINGLE_LETTER = "/n ";
    private static final String FULL_COMMAND = "/notify ";

    @Override
    public SendWebhookCommand parse(@NonNull String messageText) {
        Optional<DisputeInfoDto> disputeInfo = TextParsingUtil.getDisputeInfo(messageText);
        if (disputeInfo.isEmpty()) {
            return SendWebhookCommand.builder()
                    .validationError(CommandValidationError.INVALID_COMMAND_FORMAT)
                    .build();
        }

        return SendWebhookCommand.builder()
                .disputeInfo(disputeInfo.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 