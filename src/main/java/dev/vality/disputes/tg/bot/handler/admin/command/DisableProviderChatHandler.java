package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.DisableProviderChatCommandParser;
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


    private final AdminChatProperties adminChatProperties;
    private final ProviderChatDao providerChatDao;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final DisableProviderChatCommandParser disableProviderChatCommandParser;

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

        return isChatManagementTopic && disableProviderChatCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {
        log.info("[{}] Processing disable provider chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var disableProviderChatCommand = disableProviderChatCommandParser.parse(messageText);
        if (disableProviderChatCommand.hasValidationError()) {
            var error = disableProviderChatCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    disableProviderChatCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }
        if (disableProviderChatCommand.getTerminalId() != null) {
            providerChatDao.disable(disableProviderChatCommand.getProviderId(),
                    disableProviderChatCommand.getTerminalId());
        } else {
            providerChatDao.disable(disableProviderChatCommand.getProviderId());
        }
        log.info("[{}] Provider chat binding disabled {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }

}
