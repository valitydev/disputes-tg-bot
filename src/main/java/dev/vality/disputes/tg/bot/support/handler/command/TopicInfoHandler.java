package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicInfoHandler implements SupportMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/i";
    private static final String FULL_COMMAND = "/info";

    private final SupportChatProperties supportChatProperties;
    private final Polyglot polyglot;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(Update update) {
        if (!update.hasMessage() || update.getMessage().getMessageThreadId() == null) {
            return false;
        }

        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        return LATIN_SINGLE_LETTER.equals(messageText)
                || FULL_COMMAND.equals(messageText);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.debug("Start processing info command: {}", update);
        var chat = update.getMessage().getChat();
        var threadId = update.getMessage().getMessageThreadId();
        String text = polyglot.getText("support.info.topic-info",
                chat.getTitle(), chat.getId(), threadId);
        var response = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), text);
        response.setMessageThreadId(threadId);
        telegramClient.execute(response);
    }
}
