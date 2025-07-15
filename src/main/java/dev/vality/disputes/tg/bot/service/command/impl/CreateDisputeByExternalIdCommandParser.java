package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.CreateDisputeByExternalIdCommand;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateDisputeByExternalIdCommandParser implements CommandParser<CreateDisputeByExternalIdCommand> {

    private static final String LATIN_SINGLE_LETTER = "/e ";
    private static final String CYRILLIC_SINGLE_LETTER = "/е ";
    private static final String FULL_COMMAND = "/ext ";

    @Override
    public boolean canParse(String messageText) {
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    public CreateDisputeByExternalIdCommand parse(String messageText) {
        // Извлекаем external ID
        String externalId = TextParsingUtil.getExternalId(messageText).orElse(null);
        if (externalId == null || externalId.trim().isEmpty()) {
            return CreateDisputeByExternalIdCommand.builder()
                    .externalId(externalId)
                    .validationError(CommandValidationError.INVALID_EXTERNAL_ID)
                    .build();
        }

        return CreateDisputeByExternalIdCommand.builder()
                .externalId(externalId)
                .build();
    }
} 