package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.AddProviderChatCommandParser;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddProviderChatHandler implements AdminMessageHandler {


    private final AdminChatProperties adminChatProperties;
    private final ProviderChatDao providerChatDao;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final AddProviderChatCommandParser addProviderChatCommandParser;

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

        return isChatManagementTopic && addProviderChatCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {
        log.info("[{}] Processing add provider chat binding request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var addProviderChatCommand = addProviderChatCommandParser.parse(messageText);
        if (addProviderChatCommand.hasValidationError()) {
            var error = addProviderChatCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    addProviderChatCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        var existingChat = providerChatDao.getByProviderIdAndTerminalId(
                addProviderChatCommand.getProviderId(),
                addProviderChatCommand.getTerminalId());

        if (existingChat.isPresent()) {
            log.warn("Provider has another enabled chat");
            String replyText = polyglot.getText(polyglot.getLocale(), "error.input.provider-has-chat");
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        ProviderChat providerChat = new ProviderChat();
        providerChat.setProviderId(addProviderChatCommand.getProviderId());
        providerChat.setSendToChatId(addProviderChatCommand.getSendToChatId());
        providerChat.setSendToTopicId(addProviderChatCommand.getSendToTopicId());
        providerChat.setReadFromChatId(addProviderChatCommand.getReadFromChatId());
        providerChat.setTemplate(addProviderChatCommand.getTemplate());

        providerChatDao.save(providerChat);
        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }

}
