package dev.vality.disputes.tg.bot.support.handler.callback;

import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackHandler implements SupportMessageHandler {

    @Override
    public boolean filter(Update message) {
        // Has callback data
        return message.hasCallbackQuery();
    }

    @Override
    public void handle(Update message) {
        log.warn("This handler is not implemented.");
    }

}
