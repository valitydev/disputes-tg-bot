package dev.vality.disputes.tg.bot.provider.handler;

import dev.vality.disputes.tg.bot.core.dto.ProviderMessageDto;

public interface ProviderMessageHandler {

    boolean filter(final ProviderMessageDto message);

    void handle(ProviderMessageDto message);
}
