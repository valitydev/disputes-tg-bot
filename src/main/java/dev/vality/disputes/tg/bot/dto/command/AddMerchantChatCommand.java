package dev.vality.disputes.tg.bot.dto.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AddMerchantChatCommand extends BaseCommand {

    private final Long chatId;
    @Nullable
    private final String template;
}
