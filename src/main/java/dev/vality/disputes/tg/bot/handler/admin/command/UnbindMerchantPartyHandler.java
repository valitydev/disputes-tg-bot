package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.UnbindMerchantPartyCommandParser;
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
public class UnbindMerchantPartyHandler implements AdminMessageHandler {



    private final Polyglot polyglot;
    private final MerchantPartyDao merchantPartyDao;
    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final UnbindMerchantPartyCommandParser unbindMerchantPartyCommandParser;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        boolean isChatManagementTopic = adminChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && unbindMerchantPartyCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {
        log.info("Processing unbind merchant party request");
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var unbindMerchantPartyCommand = unbindMerchantPartyCommandParser.parse(messageText);
        if (unbindMerchantPartyCommand.hasValidationError()) {
            var error = unbindMerchantPartyCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    unbindMerchantPartyCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        try {
            if (unbindMerchantPartyCommand.getUnbindType().isAllParties()) {
                merchantPartyDao.deleteAllParties(unbindMerchantPartyCommand.getMerchantChat().getId());
            } else {
                merchantPartyDao.deleteParty(unbindMerchantPartyCommand.getMerchantChat().getId(), 
                        unbindMerchantPartyCommand.getPartyId());
            }
            log.info("Successfully unbound party ID: {} from chat ID: {}", 
                    unbindMerchantPartyCommand.getPartyId(),
                    unbindMerchantPartyCommand.getMerchantChat().getChatId());
            telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
        } catch (Exception e) {
            log.error("Failed to apply merchant party unbinding", e);
            String replyText = polyglot.getText(polyglot.getLocale(), "error.processing.failed");
            telegramApiService.sendReplyTo(replyText, update);
        }
    }
} 