package dev.vality.disputes.tg.bot.common.util;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;

import java.util.List;

@UtilityClass
public class TelegramUtil {

    private static final String HTML_PARSE_MODE = "HTML";

    public static SendMessage buildPlainTextResponse(Update update, String text) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .replyToMessageId(update.getMessage().getMessageId())
                .parseMode(HTML_PARSE_MODE)
                .text(text)
                .build();
    }

    public static SendMessage buildPlainTextResponse(Long chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .parseMode(HTML_PARSE_MODE)
                .text(text)
                .build();
    }

    public static SendMessage buildPlainTextResponse(Long chatId, String text, Integer replyToMessage) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(replyToMessage)
                .parseMode(HTML_PARSE_MODE)
                .text(text)
                .build();
    }

    public static SendDocument buildTextWithAttachmentResponse(Long chatId, String text, InputFile file) {
        return SendDocument.builder()
                .chatId(chatId)
                .parseMode(HTML_PARSE_MODE)
                .document(file)
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

        return null;
    }

    public static Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }

        if (update.hasMyChatMember()) {
            return update.getMyChatMember().getChat().getId();
        }
        return null;
    }

    public static String extractText(Update update) {
        if (!update.hasMessage()) {
            return null;
        }
        var message = update.getMessage();
        if (message.hasText()) {
            return message.getText();
        }
        if (message.getCaption() != null) {
            return message.getCaption();
        }
        return null;
    }

    public static boolean hasAttachment(Update update) {
        return update.hasMessage()
                && (update.getMessage().hasPhoto() || update.getMessage().hasDocument());
    }

    public static SetMessageReaction getSetMessageReaction(Long chatId, Integer messageId, String emoji) {
        var reaction = ReactionTypeEmoji.builder().emoji(emoji).build();
        var messageReaction = new SetMessageReaction();
        messageReaction.setChatId(chatId);
        messageReaction.setMessageId(messageId);
        messageReaction.setReactionTypes(List.of(reaction));
        return messageReaction;
    }
}
