package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddProviderChatHandler implements AdminMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/ap ";
    private static final String FULL_COMMAND = "/add_provider ";

    private final AdminChatProperties adminChatProperties;
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
        boolean isChatManagementTopic = adminChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && (messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND));
    }

    @Override
    @SneakyThrows
    @Transactional
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
            var response = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), e.getMessage());
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
