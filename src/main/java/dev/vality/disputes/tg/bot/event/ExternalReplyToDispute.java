package dev.vality.disputes.tg.bot.event;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.dto.ProviderMessageDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalReplyToDispute {
    private ProviderDispute providerDispute;
    private ProviderMessageDto message;
}
