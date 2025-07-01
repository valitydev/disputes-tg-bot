package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.tg.bot.core.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.core.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantParty;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import dev.vality.disputes.tg.bot.support.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.Locale;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class BindMerchantPartyHandler implements SupportMessageHandler {

    private static final String COMMAND = "/bind";
    private static final String SHORT_COMMAND = "/b";

    private final Polyglot polyglot;
    private final TelegramClient telegramClient;
    private final MerchantChatDao merchantChatDao;
    private final MerchantPartyDao merchantPartyDao;
    private final SupportChatProperties supportChatProperties;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(COMMAND) || messageText.startsWith(SHORT_COMMAND);
    }

    @Override
    public void handle(Update update) {
        log.info("Processing bind merchant party request");
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale();

        // Валидация команды
        String[] parts = messageText.split("\\s+", 3);
        if (parts.length < 3) {
            log.warn("Invalid command format: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.invalid-command");
            try {
                telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            } catch (Exception e) {
                log.error("Failed to send error message", e);
            }
            return;
        }

        String chatIdStr = parts[1];
        String partyId = parts[2];

        // Валидация chat_id
        Long chatId;
        try {
            chatId = CommandValidationUtil.extractLong(chatIdStr, "Chat ID");
        } catch (Exception e) {
            log.warn("Invalid chat ID: {}", chatIdStr);
            String reply = polyglot.getText(replyLocale, "error.input.invalid-chat-id");
            try {
                telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
            return;
        }

        // Проверяем существование чата в Telegram
        try {
            telegramClient.execute(new GetChat(chatId.toString()));
        } catch (Exception e) {
            log.warn("Chat not found in Telegram: {}", chatId, e);
            String reply = polyglot.getText(replyLocale, "error.chat.not-found");
            try {
                telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
            return;
        }

        // Получаем merchant_chat из БД
        var merchantChat = merchantChatDao.get(chatId);
        if (merchantChat.isEmpty()) {
            log.warn("Merchant chat not found for chat ID: {}", chatId);
            String reply = polyglot.getText(replyLocale, "error.chat.not-found");
            try {
                telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            } catch (Exception e) {
                log.error("Failed to send error message", e);
            }
            return;
        }

        // Создаем привязку party_id к чату
        MerchantParty merchantParty = new MerchantParty();
        merchantParty.setMerchantChatId(merchantChat.get().getId());
        merchantParty.setPartyId(partyId);
        merchantParty.setCreatedAt(LocalDateTime.now());

        try {
            merchantPartyDao.save(merchantParty);
            log.info("Successfully bound party ID: {} to chat ID: {}", partyId, chatId);
            String reply = polyglot.getText(replyLocale, "success.party.bound", partyId, chatId);
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
        } catch (Exception e) {
            log.error("Failed to save merchant party binding", e);
            String reply = polyglot.getText(replyLocale, "error.processing.failed");
            try {
                telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
} 