package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dto.command.TopicInfoCommand;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicInfoCommandParser implements CommandParser<TopicInfoCommand> {

    private static final String LATIN_SINGLE_LETTER = "/i";
    private static final String FULL_COMMAND = "/info";

    @Override
    public TopicInfoCommand parse(@NonNull String messageText) {
        // Command has no args
        return TopicInfoCommand.builder()
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return LATIN_SINGLE_LETTER.equals(commandString) || FULL_COMMAND.equals(commandString);
    }

} 