package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.disputes.tg.bot.merchant.dto.MerchantMessageDto;

public interface MerchantMessageHandler {

    boolean filter(final MerchantMessageDto message);

    void handle(MerchantMessageDto message);
}
