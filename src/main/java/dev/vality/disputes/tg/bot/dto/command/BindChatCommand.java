package dev.vality.disputes.tg.bot.dto.command;

import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BindChatCommand extends BaseCommand {
    private final String partyId;
    private final MerchantChat merchantChat;
}
