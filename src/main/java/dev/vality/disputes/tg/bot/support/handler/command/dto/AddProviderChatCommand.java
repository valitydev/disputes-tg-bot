package dev.vality.disputes.tg.bot.support.handler.command.dto;

import jakarta.annotation.Nullable;
import lombok.Data;

@Data
public class AddProviderChatCommand {

    private Integer providerId;
    private Long sendToChatId;
    @Nullable
    private Integer sendToThreadId;
    private Long readFromChatId;
    @Nullable
    private Integer readFromTopicId;
    @Nullable
    private String template;
}
