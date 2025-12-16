package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.DisableMerchantChatCommandParser;
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
public class DisableMerchantChatHandler implements AdminMessageHandler {


    private final AdminChatProperties adminChatProperties;
    private final MerchantChatDao merchantChatDao;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final DisableMerchantChatCommandParser disableMerchantChatCommandParser;

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

        return isChatManagementTopic && disableMerchantChatCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {
        log.info("[{}] Processing disable merchant chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var disableMerchantChatCommand = disableMerchantChatCommandParser.parse(messageText);
        if (disableMerchantChatCommand.hasValidationError()) {
            var error = disableMerchantChatCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    disableMerchantChatCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        merchantChatDao.disable(disableMerchantChatCommand.getChatId());
        log.info("[{}] Chat disabled successfully", update.getUpdateId());
        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }

}
