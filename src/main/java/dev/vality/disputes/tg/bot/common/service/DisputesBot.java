package dev.vality.disputes.tg.bot.common.service;

import dev.vality.disputes.tg.bot.common.config.properties.BotProperties;
import dev.vality.disputes.tg.bot.common.dao.ChatDao;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.getChatId;
import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.getChatTitle;

@Slf4j
public class DisputesBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final List<CommonHandler> handlers;
    private final ChatDao chatDao;

    public DisputesBot(BotProperties botProperties, List<CommonHandler> handlers, ChatDao chatDao) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.handlers = handlers;
        this.chatDao = chatDao;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("[{}] Update received: {}", update.getUpdateId(), update);
        var messageDtoBuilder = MessageDto.builder();
        messageDtoBuilder.update(update);

        Long chatId = getChatId(update);
        String chatTitle = getChatTitle(update);
        Optional<Chat> optionalChat = chatDao.get(chatId);
        if (optionalChat.isEmpty()) {
            log.info("[{}] Unknown chat with id: '{}' and title: '{}'", update.getUpdateId(), chatId, chatTitle);
            Chat chat = new Chat();
            chat.setId(chatId);
            messageDtoBuilder.chat(chat);
            return;
        } else {
            messageDtoBuilder.chat(optionalChat.get());
        }

        log.info("[{}] Processing update from chat with id: '{}' and title: '{}'",
                update.getUpdateId(), chatId, chatTitle);
        var messageDto = messageDtoBuilder.build();
        handlers.stream()
                .filter(handler -> handler.filter(messageDto))
                .findFirst().ifPresentOrElse(handler -> handler.handle(messageDto, this),
                        () -> log.info("[{}] No suitable handler found for update: {}", update.getUpdateId(),
                                messageDto));
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }
}
