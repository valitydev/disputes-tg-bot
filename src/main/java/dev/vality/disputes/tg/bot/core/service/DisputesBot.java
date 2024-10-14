package dev.vality.disputes.tg.bot.core.service;

import dev.vality.disputes.tg.bot.core.config.properties.BotProperties;
import dev.vality.disputes.tg.bot.core.handler.TelegramEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final BotProperties botProperties;
    private final List<TelegramEventHandler> handlers;

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return (List<Update> updates) ->  updates.forEach(this::consume);
    }

    @Override
    public void consume(Update update) {
        log.debug("[{}] Update received: {}", update.getUpdateId(), update);
        var applicableHandlers = handlers.stream()
                .filter(handler -> handler.filter(update)).collect(Collectors.toSet());
        applicableHandlers.forEach(
                handler -> handler.handle(update));
        if (applicableHandlers.isEmpty()) {
            log.info("[{}] No suitable handler found for update: {}", update.getUpdateId(), update);
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Registered bot running state is: {}",  botSession.isRunning());
    }
}
