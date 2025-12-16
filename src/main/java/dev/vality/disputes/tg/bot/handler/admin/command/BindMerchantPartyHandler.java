package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantParty;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.BindChatCommandParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.Locale;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class BindMerchantPartyHandler implements AdminMessageHandler {

    private final Polyglot polyglot;
    private final MerchantPartyDao merchantPartyDao;
    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final BindChatCommandParser bindChatCommandParser;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        boolean isChatManagementTopic = adminChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && bindChatCommandParser.canParse(messageText);
    }

    @Override
    @Transactional
    public void handle(Update update) {
        log.info("Processing bind merchant party request");
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale();

        var bindChatCommand = bindChatCommandParser.parse(messageText);

        // Проверяем ошибки валидации
        if (bindChatCommand.hasValidationError()) {
            var error = bindChatCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error, bindChatCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(replyLocale, error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }
        MerchantParty merchantParty = new MerchantParty();
        merchantParty.setMerchantChatId(bindChatCommand.getMerchantChat().getId());
        merchantParty.setPartyId(bindChatCommand.getPartyId());
        merchantParty.setCreatedAt(LocalDateTime.now());

        try {
            merchantPartyDao.save(merchantParty);
            log.info("Successfully bound party ID: {} to chat ID: {}", bindChatCommand.getPartyId(),
                    bindChatCommand.getMerchantChat().getChatId());
            telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
        } catch (Exception e) {
            log.error("Failed to save merchant party binding", e);
            String replyText = polyglot.getText(replyLocale, "error.processing.failed");
            telegramApiService.sendReplyTo(replyText, update);
        }
    }


} 