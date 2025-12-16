package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.TopicInfoCommandParser;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicInfoHandler implements AdminMessageHandler {



    private final AdminChatProperties adminChatProperties;
    private final Polyglot polyglot;
    private final TelegramApiService telegramApiService;
    private final TopicInfoCommandParser topicInfoCommandParser;

    @Override
    public boolean filter(Update update) {
        if (!update.hasMessage() || update.getMessage().getMessageThreadId() == null) {
            return false;
        }

        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        return topicInfoCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.debug("Start processing info command: {}", update);
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var topicInfoCommand = topicInfoCommandParser.parse(messageText);
        if (topicInfoCommand.hasValidationError()) {
            var error = topicInfoCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    topicInfoCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        var chat = update.getMessage().getChat();
        var threadId = update.getMessage().getMessageThreadId();
        String text = polyglot.getText("support.info.topic-info",
                chat.getTitle(), chat.getId().toString(), threadId);
        var response = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), text);
        response.setMessageThreadId(threadId);
        telegramApiService.sendMessage(text, adminChatProperties.getId(), threadId);
    }
}
