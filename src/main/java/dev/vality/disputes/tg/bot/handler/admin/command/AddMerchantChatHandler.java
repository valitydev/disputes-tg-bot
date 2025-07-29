package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.AddMerchantChatCommandParser;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddMerchantChatHandler implements AdminMessageHandler {

    private final MerchantChatDao merchantChatDao;
    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final AddMerchantChatCommandParser addMerchantChatCommandParser;

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

        return isChatManagementTopic && addMerchantChatCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing add merchant chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var addMerchantChatCommand = addMerchantChatCommandParser.parse(messageText);
        // Проверяем ошибки валидации
        if (addMerchantChatCommand.hasValidationError()) {
            var error = addMerchantChatCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    addMerchantChatCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }
        
        var chatInfoOpt = telegramApiService.getChatInfo(addMerchantChatCommand.getChatId());
        if (chatInfoOpt.isEmpty()) {
            log.warn("Chat not found in Telegram: {}", addMerchantChatCommand.getChatId());
            String replyText = polyglot.getText("error.chat.not-found");
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }
        var chatInfo = chatInfoOpt.get();
        log.info("[{}] Got chat from telegram: {}", update.getUpdateId(), chatInfo);

        // Проверяем, что не существует активного чата с таким ID
        var existingChatOpt = merchantChatDao.get(chatInfo.getId());
        if (existingChatOpt.isPresent()) {
            log.warn("Merchant chat already exists and is enabled: {}", chatInfo.getId());
            String replyText = polyglot.getText("error.merchant-chat.already-exists");
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        MerchantChat merchantChat = new MerchantChat();
        merchantChat.setChatId(chatInfo.getId());
        merchantChat.setTitle(chatInfo.getTitle());
        long chatId = merchantChatDao.save(merchantChat);
        log.info("Chat saved successfully with id: {}", chatId);

        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }


}
