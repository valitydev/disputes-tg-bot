package dev.vality.disputes.tg.bot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddMerchantChatCommand {

    private final long chatId;
}
