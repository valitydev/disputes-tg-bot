package dev.vality.disputes.tg.bot.test;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Random;

@UtilityClass
public class TelegramObjectUtil {

    public static final String ADD_MERCHANT_CHAT_COMMAND = "/am %d";
    public static final String DISABLE_MERCHANT_CHAT_COMMAND = "/dm %d";

    public static Update buildAddMerchantChatCommand(long chatId) {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setText(ADD_MERCHANT_CHAT_COMMAND.formatted(chatId));
        update.setMessage(message);
        return update;
    }

    public static Update buildDisableMerchantChatCommand(long chatId) {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setText(DISABLE_MERCHANT_CHAT_COMMAND.formatted(chatId));
        update.setMessage(message);
        return update;
    }

    public static Update buildSimpleTextMessage() {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        update.setMessage(buildMessage());
        return update;
    }

    public static ChatFullInfo buildChatFullInfo(long chatId) {
        var chatInfo = new ChatFullInfo();
        chatInfo.setId(chatId);
        return chatInfo;
    }

    private static Message buildMessage() {
        Message message = new Message();
        message.setMessageId(new Random().nextInt());
        message.setChat(buildChat());
        return message;
    }

    private static Chat buildChat() {
        return Chat.builder()
                .id(new Random().nextLong())
                .title("Test chat")
                .type("supergroup")
                .build();
    }
}
