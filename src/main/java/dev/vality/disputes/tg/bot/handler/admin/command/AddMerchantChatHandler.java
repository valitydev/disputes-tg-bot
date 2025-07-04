package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.dto.AddMerchantChatCommand;
import dev.vality.disputes.tg.bot.exception.CommandValidationException;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddMerchantChatHandler implements AdminMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/am ";
    private static final String FULL_COMMAND = "/add_merch ";

    private final MerchantChatDao merchantChatDao;
    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage() || update.getMessage().getMessageThreadId() == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        boolean isChatManagementTopic = adminChatProperties.getTopics().getChatManagement().equals(threadId);

        return isChatManagementTopic && (messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND));
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing add merchant chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);
        var addMerchantChatCommandOpt = parse(update);
        if (addMerchantChatCommandOpt.isEmpty()) {
            log.warn("Unable to parse addMerchantChatCommand, check previous logs");
            return;
        }
        var addMerchantChatCommand = addMerchantChatCommandOpt.get();
        var chatInfoOpt = telegramApiService.getChatInfo(addMerchantChatCommand.getChatId());
        if (chatInfoOpt.isEmpty()) {
            log.warn("Chat not found in Telegram: {}", addMerchantChatCommand.getChatId());
            String replyText = polyglot.getText("error.chat.not-found");
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }
        var chatInfo = chatInfoOpt.get();
        log.info("[{}] Got chat from telegram: {}", update.getUpdateId(), chatInfo);

        MerchantChat merchantChat = new MerchantChat();
        merchantChat.setChatId(chatInfo.getId());
        merchantChat.setTitle(chatInfo.getTitle());
        long chatId = merchantChatDao.save(merchantChat);
        log.info("Chat saved successfully with id: {}", chatId);

        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }

    private Optional<AddMerchantChatCommand> parse(Update update) {
        String messageText = extractText(update);
        String[] parts = messageText.split("\\s+", 3);
        if (parts.length < 2) {
            log.warn("Invalid command format: {}", messageText);
            String replyText = polyglot.getText("error.input.invalid-add-merchant-chat-command");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
        try {
            long chatId = CommandValidationUtil.extractLong(parts[1], "Chat ID");
            return Optional.of(AddMerchantChatCommand.builder().chatId(chatId).build());
        } catch (CommandValidationException e) {
            log.warn("Invalid chat ID: {}", parts[1], e);
            String replyText = polyglot.getText("error.input.invalid-chat-id");
            telegramApiService.sendReplyTo(replyText, update);
            return Optional.empty();
        }
    }

}
