package dev.vality.disputes.tg.bot.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.vality.disputes.tg.bot.handler.TelegramEventHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisputesBotTest {

    @Test
    void processesGroupsOnceAtTheirFirstPosition() {
        var handler = mock(TelegramEventHandler.class);
        when(handler.filter(any())).thenReturn(true);
        var bot = new DisputesBot(null, List.of(handler));
        var first = update("group-1");
        var single = update(null);
        var second = update("group-2");
        var firstLast = update("group-1");
        var secondLast = update("group-2");

        bot.consume(List.of(first, single, second, firstLast, secondLast));

        var order = inOrder(handler);
        order.verify(handler).handle(List.of(first, firstLast));
        order.verify(handler).handle(single);
        order.verify(handler).handle(List.of(second, secondLast));
    }

    @Test
    void warnsOnlyForSingleAlbumElementInBatch() {
        var logger = (Logger) LoggerFactory.getLogger(DisputesBot.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var bot = new DisputesBot(null, List.of());
            bot.consume(List.of(update("group-1")));
            bot.consume(List.of(update("group-2"), update("group-2"), update(null)));

            var warnings = appender.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
            assertEquals(1, warnings.size());
            assertEquals("Media group group-1 has only one update in the current batch; " +
                    "the album may be split across batches", warnings.getFirst().getFormattedMessage());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private Update update(String mediaGroupId) {
        var update = mock(Update.class);
        var message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getMediaGroupId()).thenReturn(mediaGroupId);
        return update;
    }
}
