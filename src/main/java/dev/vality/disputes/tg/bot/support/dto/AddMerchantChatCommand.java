package dev.vality.disputes.tg.bot.support.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddMerchantChatCommand {

    private final long chatId;
}
