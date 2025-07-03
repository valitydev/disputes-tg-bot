package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.tg.bot.dto.File;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class TelegramUtil {

    private static final String htmlParseMode = "HTML";

    public static SendMessage buildPlainTextResponse(Update update, String text) {
        return SendMessage.builder()
                .chatId(Objects.requireNonNull(getChatId(update)))
                .replyToMessageId(getMessageId(update))
                .messageThreadId(getMessageThreadId(update))
                .parseMode(htmlParseMode)
                .text(text)
                .build();
    }

    public static SendMessage buildPlainTextResponse(Long chatId, String text) {
        return buildPlainTextResponse(chatId, null, text);
    }

    public static SendMessage buildPlainTextResponse(Long chatId, Integer threadId, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .parseMode(htmlParseMode)
                .text(text)
                .build();
    }

    public static SendMessage buildPlainTextResponse(Long chatId, String text, Integer replyToMessage) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(replyToMessage)
                .parseMode(htmlParseMode)
                .text(text)
                .build();
    }

    private static Integer getMessageId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getMessageId();
        }
        if (update.hasChannelPost()) {
            return update.getChannelPost().getMessageId();
        }
        return null;
    }

    private static Integer getMessageThreadId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getMessageThreadId();
        }
        if (update.hasChannelPost()) {
            return update.getChannelPost().getMessageThreadId();
        }
        return null;
    }

    public static SendDocument buildTextWithAttachmentResponse(Long chatId, String text, InputFile attachment) {
        return SendDocument.builder()
                .chatId(chatId)
                .parseMode(htmlParseMode)
                .document(attachment)
                .caption(text)
                .build();
    }

    public static SendDocument buildTextWithAttachmentResponse(Long chatId, Integer threadId, String text,
                                                               InputFile attachment) {
        return SendDocument.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .parseMode(htmlParseMode)
                .document(attachment)
                .caption(text)
                .build();
    }

    public static String getChatTitle(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChat().getTitle();
        }

        if (update.hasMyChatMember()) {
            return update.getMyChatMember().getChat().getTitle();
        }

        if (update.hasCallbackQuery()
                && update.getCallbackQuery().getMessage() != null
                && update.getCallbackQuery().getMessage() instanceof Message) {
            return update.getCallbackQuery().getMessage().getChat().getTitle();
        }

        return null;
    }

    public static Long getChatId(Update update) {
        if (update.hasMessage() && update.getMessage().getChatId() != null) {
            return update.getMessage().getChatId();
        }

        if (update.hasChannelPost() && update.getChannelPost().getChatId() != null) {
            return update.getChannelPost().getChatId();
        }

        if (update.hasMyChatMember() && update.getMyChatMember().getChat() != null) {
            return update.getMyChatMember().getChat().getId();
        }

        if (update.hasCallbackQuery()
                && update.getCallbackQuery().getMessage() != null
                && update.getCallbackQuery().getMessage().getChatId() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    public static String extractText(Update update) {
        var message = getMessage(update);
        if (message.hasText()) {
            return message.getText();
        }
        if (message.getCaption() != null) {
            return message.getCaption();
        }
        return null;
    }

    public static String extractUserInfo(Update update) {
        var user = getUser(update);
        if (user == null) {
            return null;
        }
        return String.format("[%s] %s %s (%d)", user.getUserName(), user.getFirstName(), user.getLastName(),
                user.getId());
    }

    public static User getUser(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom();
        }
        if (update.hasChannelPost()) {
            return update.getChannelPost().getFrom();
        }
        return null;
    }

    public static boolean hasAttachment(Update update) {
        if (update == null) {
            return false;
        }
        var message = getMessage(update);
        return hasAttachment(message);
    }

    public static boolean hasAttachment(Message message) {
        if (message == null) {
            return false;
        }
        return message.hasPhoto() || message.hasDocument() || message.hasVideo();
    }

    public static Message getMessage(Update update) {
        if (update.hasMessage()) {
            return update.getMessage();
        } else if (update.hasChannelPost()) {
            return update.getChannelPost();
        } else {
            return null;
        }
    }

    public static SetMessageReaction getSetMessageReaction(Long chatId, Integer messageId, String emoji) {
        var reaction = ReactionTypeEmoji.builder().emoji(emoji).build();
        var messageReaction = new SetMessageReaction(String.valueOf(chatId), messageId);
        messageReaction.setReactionTypes(List.of(reaction));
        return messageReaction;
    }

    public static InputFile convertToInputFile(File document, String fileName) {
        InputFile file = new InputFile();
        file.setMedia(new ByteArrayInputStream(document.getContent()), fileName);
        return file;
    }
}
