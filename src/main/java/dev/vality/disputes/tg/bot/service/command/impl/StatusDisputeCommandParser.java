package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.command.StatusDisputeCommand;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StatusDisputeCommandParser implements CommandParser<StatusDisputeCommand> {

    private static final String LATIN_SINGLE_LETTER = "/s ";
    private static final String FULL_COMMAND = "/status ";

    @Override
    public boolean canParse(String messageText) {
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    public StatusDisputeCommand parse(String messageText) {
        // Извлекаем информацию о споре
        Optional<DisputeInfoDto> disputeInfoOptional = TextParsingUtil.getDisputeInfo(messageText);
        
        if (disputeInfoOptional.isEmpty()) {
            return StatusDisputeCommand.builder()
                    .disputeInfo(null)
                    .validationError(CommandValidationError.INVALID_COMMAND_FORMAT)
                    .build();
        }

        return StatusDisputeCommand.builder()
                .disputeInfo(disputeInfoOptional.get())
                .build();
    }
} 