package dev.vality.disputes.tg.bot.handler;

import dev.vality.disputes.tg.bot.dto.ProviderMessageDto;

public interface ProviderMessageHandler {

    boolean filter(final ProviderMessageDto message);

    void handle(ProviderMessageDto message);
}
