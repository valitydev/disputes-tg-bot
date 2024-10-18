package dev.vality.disputes.tg.bot.common.handler;

import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;

public interface CommonHandler {

    boolean filter(final MessageDto messageDto);

    void handle(MessageDto messageDto, DisputesBot disputesBot);

}
