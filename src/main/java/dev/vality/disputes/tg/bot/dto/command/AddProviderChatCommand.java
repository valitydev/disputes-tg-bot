package dev.vality.disputes.tg.bot.dto.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AddProviderChatCommand extends BaseCommand {

    private final Integer providerId;
    @Nullable
    private final Integer terminalId;
    private final Long sendToChatId;
    @Nullable
    private final Integer sendToTopicId;
    private final Long readFromChatId;
    @Nullable
    private final String template;
} 