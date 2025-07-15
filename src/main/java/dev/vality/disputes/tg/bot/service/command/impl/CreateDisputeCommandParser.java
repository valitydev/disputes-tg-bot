package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.command.CreateDisputeCommand;
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
public class CreateDisputeCommandParser implements CommandParser<CreateDisputeCommand> {

    private static final String LATIN_SINGLE_LETTER = "/c ";
    private static final String CYRILLIC_SINGLE_LETTER = "/с ";
    private static final String FULL_COMMAND = "/check ";

    @Override
    public CreateDisputeCommand parse(@NonNull String messageText) {
        Optional<DisputeInfoDto> disputeInfo = TextParsingUtil.getDisputeInfo(messageText);
        if (disputeInfo.isEmpty()) {
            return CreateDisputeCommand.builder()
                    .validationError(CommandValidationError.INVALID_COMMAND_FORMAT)
                    .build();
        }

        return CreateDisputeCommand.builder()
                .disputeInfo(disputeInfo.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER)
                || commandString.startsWith(CYRILLIC_SINGLE_LETTER)
                || commandString.startsWith(FULL_COMMAND);
    }

} 