package dev.vality.disputes.tg.bot.core.service;

import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.merchant.util.PolyglotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiService {

    private final Polyglot polyglot;
    private final TelegramClient telegramClient;

    public void setThumbUpReaction(Long chatId, Integer messageId) {
        setReaction("👍", chatId, messageId);
    }

    public void setReaction(String emoji, Long chatId, Integer messageId) {
        var reaction = TelegramUtil.getSetMessageReaction(chatId, messageId, emoji);
        try {
            telegramClient.execute(reaction);
        } catch (Exception e) {
            log.warn("Unable to set reaction to telegram message. chat_id: {}, message_id: {}", chatId, messageId, e);
        }
    }

    public Optional<ChatFullInfo> getChatInfo(Long chatId) {
        log.debug("Lookup chat with id: {}", chatId);
        try {
            return Optional.of(telegramClient.execute(new GetChat(chatId.toString())));
        } catch (Exception e) {
            log.debug("Unable to find chat with id: {}", chatId, e);
        }
        return Optional.empty();
    }

    public void sendReplyTo(Locale replyLocale, String phraseKey, Update update) {
        String reply = polyglot.getText(replyLocale, phraseKey);
        try {
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }

    public void sendReplyTo(String phraseKey, Update update) {
        String reply = polyglot.getText(phraseKey);
        try {
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }
}
