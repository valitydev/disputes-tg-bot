package dev.vality.disputes.tg.bot.support.dto;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BindChatCommand {
    private final String partyId;
    private final MerchantChat merchantChat;
}
