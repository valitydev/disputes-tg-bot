package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisableProviderChatHandler implements AdminMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/dp ";
    private static final String FULL_COMMAND = "/disable_provider ";

    private final AdminChatProperties adminChatProperties;
    private final ProviderChatDao providerChatDao;
    private final TelegramApiService telegramApiService;

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
        log.info("[{}] Processing disable provider chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        try {
            var providerId = parse(messageText);
            providerChatDao.disable(providerId);
            log.info("[{}] Provider chat binding disabled {}",
                    update.getUpdateId(), TelegramUtil.extractUserInfo(update));
            telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
        } catch (CommandValidationException e) {
            log.warn("Unable to process user input", e);
            var response = TelegramUtil.buildPlainTextResponse(adminChatProperties.getId(), e.getMessage());
            response.setReplyToMessageId(update.getMessage().getMessageId());
            response.setMessageThreadId(update.getMessage().getMessageThreadId());
            telegramApiService.sendReplyTo(e.getMessage(), update);
        }
    }

    private Integer parse(String text) throws CommandValidationException {
        String[] tokens = text.split("\\s+", 2);
        if (tokens.length < 2) {
            throw new CommandValidationException("Expected 1 arguments, got %s".formatted(tokens.length - 1));
        }
        return CommandValidationUtil.extractInteger(tokens[1], "Provider ID");
    }

}
