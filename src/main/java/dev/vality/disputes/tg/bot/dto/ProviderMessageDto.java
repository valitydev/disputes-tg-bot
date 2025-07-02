package dev.vality.disputes.tg.bot.dto;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.Update;

@Getter
@Builder
@ToString
public class ProviderMessageDto {
    private Update update;
    private ProviderChat providerChat;
}
