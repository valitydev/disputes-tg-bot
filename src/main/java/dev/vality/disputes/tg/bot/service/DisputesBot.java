package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.config.properties.BotProperties;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static dev.vality.disputes.tg.bot.util.FormatUtil.prepareForLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final BotProperties botProperties;
    private final List<TelegramEventHandler> handlers;

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(List<Update> updates) {
        var mediaGroups = groupUpdatesByMediaGroupId(updates);
        var processedMediaGroups = new HashSet<String>();
        for (var update : updates) {
            var mediaGroupId = TelegramUtil.getMediaGroupId(update);
            if (mediaGroupId == null) {
                consume(update);
            } else if (processedMediaGroups.add(mediaGroupId)) {
                consumeMediaGroup(mediaGroups.get(mediaGroupId));
            }
        }
    }

    public void consume(Update update) {
        dispatch(update, handler -> handler.handle(update));
    }

    private Map<String, List<Update>> groupUpdatesByMediaGroupId(List<Update> updates) {
        return updates.stream()
                .filter(update -> TelegramUtil.getMediaGroupId(update) != null)
                .collect(Collectors.groupingBy(
                        TelegramUtil::getMediaGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void consumeMediaGroup(List<Update> updates) {
        if (updates.size() == 1) {
            log.warn("Media group {} has only one update in the current batch, album may be split across batches",
                    TelegramUtil.getMediaGroupId(updates.getFirst()));
        }
        var primaryUpdate = updates.stream()
                .filter(update -> TelegramUtil.extractText(update) != null)
                .findFirst()
                .orElseGet(updates::getFirst);
        dispatch(primaryUpdate, handler -> handler.handle(updates));
    }

    private void dispatch(Update update, Consumer<TelegramEventHandler> action) {
        try {
            log.debug("[{}] Update received: {}", update.getUpdateId(), prepareForLog(update.toString()));
            var applicableHandlers = handlers.stream()
                    .filter(handler -> handler.filter(update)).collect(Collectors.toSet());
            log.debug("[{}] Found {} applicable root handlers", update.getUpdateId(), applicableHandlers.size());
            applicableHandlers.forEach(action);
            if (applicableHandlers.isEmpty()) {
                log.info("[{}] No suitable handler found for update: {}",
                        update.getUpdateId(), prepareForLog(update.toString()));
            }
        } catch (Exception e) {
            log.error("[{}] Unexpected error occurred: ", update.getUpdateId(), e);
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Registered bot running state is: {}",  botSession.isRunning());
    }
}
