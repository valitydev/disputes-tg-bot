package dev.vality.disputes.tg.bot.test;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Random;

@UtilityClass
public class TelegramObjectUtil {

    public static final String ADD_MERCHANT_CHAT_COMMAND = "/am %d";
    public static final String DISABLE_MERCHANT_CHAT_COMMAND = "/dm %d";
    public static final String TEST_INVOICE_ID = "11Testtt1h1";
    public static final String MERCHANT_CREATE_DISPUTE_COMMAND = "/c " + TEST_INVOICE_ID;
    public static final String MERCHANT_STATUS_DISPUTE_COMMAND = "/s " + TEST_INVOICE_ID;
    public static final String MERCHANT_STATUS_DISPUTE_BY_ID_COMMAND = "/s %s";
    public static final String NON_EXISTING_INVOICE_ID = "nonExistingInvoice123";
    public static final String MERCHANT_STATUS_NON_EXISTING_DISPUTE_COMMAND = "/s " + NON_EXISTING_INVOICE_ID;
    
    // Document attachment constants
    public static final String TEST_FILE_ID = "test-file-id";
    public static final String TEST_FILE_NAME = "test-attachment.pdf";
    public static final String TEST_MIME_TYPE = "application/pdf";
    public static final Long TEST_FILE_SIZE = 1024L;
    
    // Chat constants
    public static final String TEST_CHAT_TITLE = "Test chat";
    public static final String TEST_CHAT_TYPE = "supergroup";
    
    public static long generateRandomChatId() {
        return new Random().nextLong();
    }

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

    public static Update buildMerchantStatusDisputeCommand() {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setText(MERCHANT_STATUS_DISPUTE_COMMAND);
        update.setMessage(message);
        return update;
    }

    public static Update buildMerchantStatusDisputeByIdCommand(String disputeId) {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setText(MERCHANT_STATUS_DISPUTE_BY_ID_COMMAND.formatted(disputeId));
        update.setMessage(message);
        return update;
    }

    public static Update buildMerchantStatusNonExistingDisputeCommand() {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setText(MERCHANT_STATUS_NON_EXISTING_DISPUTE_COMMAND);
        update.setMessage(message);
        return update;
    }

    public static Update buildMerchantCreateDisputeCommand() {
        var update = new Update();
        update.setUpdateId(new Random().nextInt());
        var message = buildMessage();
        message.setCaption(MERCHANT_CREATE_DISPUTE_COMMAND);
        
        // Add document attachment
        var document = new Document();
        document.setFileId(TEST_FILE_ID);
        document.setFileName(TEST_FILE_NAME);
        document.setMimeType(TEST_MIME_TYPE);
        document.setFileSize(TEST_FILE_SIZE);
        message.setDocument(document);
        
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
                .title(TEST_CHAT_TITLE)
                .type(TEST_CHAT_TYPE)
                .build();
    }
}
