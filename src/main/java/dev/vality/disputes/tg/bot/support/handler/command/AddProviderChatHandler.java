package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.provider.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import dev.vality.disputes.tg.bot.support.util.CommandValidationUtil;
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
public class AddProviderChatHandler implements SupportMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/ap ";
    private static final String FULL_COMMAND = "/add_provider ";

    private final SupportChatProperties supportChatProperties;
    private final ProviderChatDao providerChatDao;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage() || update.getMessage().getMessageThreadId() == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        boolean isChatManagementTopic = supportChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && (messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND));
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing add provider chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        try {
            var providerChat = parse(messageText);
            providerChatDao.save(providerChat);
            var successReaction = TelegramUtil.getSetMessageReaction(update.getMessage().getChatId(),
                    update.getMessage().getMessageId(), "👍");
            telegramClient.execute(successReaction);
        } catch (CommandValidationException e) {
            log.warn("Unable to process user input", e);
            var response = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), e.getMessage());
            response.setReplyToMessageId(update.getMessage().getMessageId());
            response.setMessageThreadId(update.getMessage().getMessageThreadId());
            telegramClient.execute(response);
        }
    }

    ProviderChat parse(String text) throws CommandValidationException {
        String[] tokens = text.split("\\s+", 6);
        if (tokens.length < 6) {
            throw new CommandValidationException("Expected 5 arguments, got %s".formatted(tokens.length - 1));
        }
        var providerId = CommandValidationUtil.extractInteger(tokens[1], "Provider ID");
        var sendToChatId = CommandValidationUtil.extractLong(tokens[2], "Send to chat ID");
        var sendToTopicId = CommandValidationUtil.extractNullableInteger(tokens[3], "Send to topic ID");
        var readFromChatId = CommandValidationUtil.extractLong(tokens[4], "Read from chat ID");
        var template = CommandValidationUtil.extractNullableString(tokens[5]);

        ProviderChat providerChat = new ProviderChat();
        providerChat.setProviderId(providerId);
        providerChat.setSendToChatId(sendToChatId);
        providerChat.setSendToTopicId(sendToTopicId);
        providerChat.setReadFromChatId(readFromChatId);
        providerChat.setTemplate(template);
        return providerChat;
    }

}
