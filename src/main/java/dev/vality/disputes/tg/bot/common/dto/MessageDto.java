package dev.vality.disputes.tg.bot.common.dto;

import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.Update;

@Getter
@Builder
@ToString
public class MessageDto {
    private Update update;
    private Chat chat;
}
