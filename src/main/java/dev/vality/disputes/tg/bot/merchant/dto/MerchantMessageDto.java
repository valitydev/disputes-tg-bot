package dev.vality.disputes.tg.bot.merchant.dto;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.Update;

@Getter
@Builder
@ToString
public class MerchantMessageDto {
    private Update update;
    private MerchantChat merchantChat;
    private List<String> permittedParties;
}
