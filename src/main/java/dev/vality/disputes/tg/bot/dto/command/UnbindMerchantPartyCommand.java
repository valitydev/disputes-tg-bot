package dev.vality.disputes.tg.bot.dto.command;

import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UnbindMerchantPartyCommand extends BaseCommand {

    private final Long chatId;
    private final String partyId;
    private final UnbindPartyType unbindType;
    private final MerchantChat merchantChat;
} 