package dev.vality.disputes.tg.bot.core.event;

import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;

@Getter
@Builder
public class NewMerchantChat {
    private final ChatFullInfo chatFullInfo;
}
