package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import dev.vality.disputes.tg.bot.support.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisableMerchantChatHandler implements SupportMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/dm ";
    private static final String FULL_COMMAND = "/disable_merch ";

    private final SupportChatProperties supportChatProperties;
    private final MerchantChatDao merchantChatDao;
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
        log.info("[{}] Processing disable merchant chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);
        try {
            var chatId = getChatId(messageText);
            var getChatRequest = new GetChat(chatId.toString());
            var chatInfo = telegramClient.execute(getChatRequest);
            log.info("[{}] Got chat form telegram: {}", update.getUpdateId(), chatInfo);
            merchantChatDao.disable(chatInfo.getId());
            log.info("[{}] Chat disabled successfully", update.getUpdateId());
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

    private Long getChatId(String text) throws CommandValidationException {
        String[] tokens = text.split("\\s+", 3);
        if (tokens.length < 2) {
            throw new CommandValidationException("Expected 1 arguments, got %s".formatted(tokens.length - 1));
        }
        return CommandValidationUtil.extractLong(tokens[1], "Chat ID");
    }

}
