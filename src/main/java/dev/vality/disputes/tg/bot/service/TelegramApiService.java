package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiService {

    private final TelegramClient telegramClient;

    public void setThumbUpReaction(Long chatId, Integer messageId) {
        setReaction("👍", chatId, messageId);
    }

    public void setReaction(String emoji, Long chatId, Integer messageId) {
        try {
            var reaction = TelegramUtil.getSetMessageReaction(chatId, messageId, emoji);
            telegramClient.execute(reaction);
        } catch (TelegramApiException e) {
            log.warn("Unable to set reaction to telegram message. chat_id: {}, message_id: {}", chatId, messageId, e);
        }
    }

    public Optional<ChatFullInfo> getChatInfo(Long chatId) {
        try {
            log.debug("Lookup chat with id: {}", chatId);
            return Optional.of(telegramClient.execute(new GetChat(chatId.toString())));
        } catch (TelegramApiException e) {
            log.debug("Unable to find chat with id: {}", chatId, e);
            return Optional.empty();
        }
    }

    public void sendReplyTo(String replyText, Update parentMessage) {
        try {
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(parentMessage, replyText));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }

    public void sendReplyTo(String replyText, Long chatId, Integer messageId) {
        try {
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(chatId, replyText, messageId));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }

    public Optional<Message> sendMessage(String messageText, Long chatId, Integer threadId) {
        try {
            return Optional.of(telegramClient.execute(TelegramUtil.buildPlainTextResponse(chatId, threadId,
                    messageText)));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
            return Optional.empty();
        }
    }

    public Optional<Message> sendMessageWithAttachment(String messageText, Long chatId, Integer threadId,
                                                       InputFile attachment) {
        try {
            var sendDocument = TelegramUtil.buildTextWithAttachmentResponse(chatId, threadId, messageText, attachment);
            return Optional.of(telegramClient.execute(sendDocument));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
            return Optional.empty();
        }
    }

    public Optional<Message> sendMessageWithAttachment(String messageText, Long chatId, InputFile attachment) {
        try {
            var sendDocument = TelegramUtil.buildTextWithAttachmentResponse(chatId, messageText, attachment);
            return Optional.of(telegramClient.execute(sendDocument));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
            return Optional.empty();
        }
    }

    public void deleteMessage(String chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
            telegramClient.execute(deleteMessage);
            log.info("Source message was deleted");
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
    }

    public byte[] getFile(String fileId) throws TelegramApiException, IOException {
        var getFileRequest = GetFile.builder()
                .fileId(fileId)
                .build();
        log.debug("Getting file info with fileId: {} from telegram", fileId);
        org.telegram.telegrambots.meta.api.objects.File file = telegramClient.execute(getFileRequest);
        log.debug("Successfully got file info with id: {} from telegram", fileId);
        return downloadFile(file.getFilePath());
    }

    public byte[] downloadFile(String filePath) throws IOException, TelegramApiException {
        log.debug("Downloading file by path: {} ", filePath);
        java.io.File attachedFile = telegramClient.downloadFile(filePath);
        log.debug("Successfully got file by path: {} ", filePath);
        return Files.readAllBytes(attachedFile.toPath());
    }
}
