package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.dto.BindChatCommand;
import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Locale;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnbindMerchantPartyHandler implements AdminMessageHandler {

    private static final String ALL_PARTIES_MASK = "all";
    private static final String FULL_COMMAND = "/unbind ";
    private static final String LATIN_SINGLE_LETTER = "/ub ";

    private final Polyglot polyglot;
    private final MerchantChatDao merchantChatDao;
    private final MerchantPartyDao merchantPartyDao;
    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        boolean isChatManagementTopic = adminChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && (messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND));
    }

    @Override
    @Transactional
    public void handle(Update update) {
        log.info("Processing unbind merchant party request");
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale();

        var bindChatCommandOpt = parse(update, messageText, replyLocale);
        if (bindChatCommandOpt.isEmpty()) {
            log.warn("Unable to parse unbindChatCommand, check previous logs");
            return;
        }

        BindChatCommand bindChatCommand = bindChatCommandOpt.get();
        try {
            if (ALL_PARTIES_MASK.equals(bindChatCommand.getPartyId())) {
                merchantPartyDao.deleteAllParties(bindChatCommand.getMerchantChat().getId());
            } else {
                merchantPartyDao.deleteParty(bindChatCommand.getMerchantChat().getId(), bindChatCommand.getPartyId());
            }
            log.info("Successfully unbound party ID: {} from chat ID: {}", bindChatCommand.getPartyId(),
                    bindChatCommand.getMerchantChat().getChatId());
            telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
        } catch (Exception e) {
            log.error("Failed to apply merchant party unbinding", e);
            String replyText = polyglot.getText(replyLocale, "error.processing.failed");
            telegramApiService.sendReplyTo(replyText, update);
        }
    }

    private Optional<BindChatCommand> parse(Update update, String messageText, Locale replyLocale) {

        String[] parts = messageText.split("\\s+", 3);
        if (parts.length < 3) {
            log.warn("Invalid command format: {}", messageText);
            String replyText = polyglot.getText(replyLocale, "error.input.invalid-unbind-chat-command");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
        Long chatId;
        try {
            chatId = CommandValidationUtil.extractLong(parts[1], "Chat ID");
        } catch (CommandValidationException e) {
            log.warn("Invalid chat ID: {}", parts[1], e);
            String replyText = polyglot.getText(replyLocale, "error.input.invalid-chat-id");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
        if (telegramApiService.getChatInfo(chatId).isEmpty()) {
            log.warn("Chat not found in Telegram: {}", chatId);
            String replyText = polyglot.getText(replyLocale, "error.chat.not-found");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
        var merchantChat = merchantChatDao.get(chatId);
        if (merchantChat.isEmpty()) {
            log.warn("Merchant chat not found for chat ID: {}", chatId);
            String replyText = polyglot.getText(replyLocale, "error.chat.not-found");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
        String partyId = parts[2];
        return Optional.ofNullable(BindChatCommand.builder()
                .partyId(partyId)
                .merchantChat(merchantChat.get())
                .build());
    }
} 