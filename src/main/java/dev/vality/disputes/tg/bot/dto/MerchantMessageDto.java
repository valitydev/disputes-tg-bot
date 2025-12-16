package dev.vality.disputes.tg.bot.dto;

import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Getter
@Builder
@ToString
public class MerchantMessageDto {
    private Update update;
    private MerchantChat merchantChat;
    private List<String> permittedParties;
}
