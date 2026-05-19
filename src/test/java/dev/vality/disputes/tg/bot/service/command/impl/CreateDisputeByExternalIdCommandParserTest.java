package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateDisputeByExternalIdCommandParserTest {

    private final CreateDisputeByExternalIdCommandParser parser = new CreateDisputeByExternalIdCommandParser();

    @Test
    void parseExternalIdWithTrailingText() {
        var command = parser.parse("/e D1473901023 #fin");

        assertEquals("D1473901023", command.getExternalId());
        assertNull(command.getValidationError());
    }

    @Test
    void parseInvalidExternalIdCommand() {
        var command = parser.parse("/e ");

        assertEquals(CommandValidationError.INVALID_EXTERNAL_ID, command.getValidationError());
    }
}
