package dev.vality.disputes.tg.bot.handler;

import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface MerchantMessageHandler {

    boolean filter(final MerchantMessageDto message);

    void handle(MerchantMessageDto message) throws TelegramApiException;
}
