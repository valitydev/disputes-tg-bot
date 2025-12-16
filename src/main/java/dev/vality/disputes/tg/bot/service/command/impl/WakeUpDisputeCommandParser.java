package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.command.WakeUpDisputeCommand;
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
public class WakeUpDisputeCommandParser implements CommandParser<WakeUpDisputeCommand> {

    private static final String SHORT_COMMAND = "/wud ";
    private static final String FULL_COMMAND = "/wake_up_dispute ";

    @Override
    public WakeUpDisputeCommand parse(@NonNull String messageText) {
        // Извлекаем информацию о споре
        Optional<DisputeInfoDto> disputeInfoOptional = TextParsingUtil.getDisputeInfo(messageText);

        if (disputeInfoOptional.isEmpty()) {
            return WakeUpDisputeCommand.builder()
                    .disputeInfo(null)
                    .validationError(CommandValidationError.INVALID_COMMAND_FORMAT)
                    .build();
        }

        return WakeUpDisputeCommand.builder()
                .disputeInfo(disputeInfoOptional.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(SHORT_COMMAND) || commandString.startsWith(FULL_COMMAND);
    }
}
