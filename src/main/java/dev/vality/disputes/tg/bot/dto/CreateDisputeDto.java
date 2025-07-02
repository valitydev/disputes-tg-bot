package dev.vality.disputes.tg.bot.dto;

import dev.vality.disputes.provider.DisputeParams;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CreateDisputeDto {
    private DisputeParams disputeParams;
    private File attachment;
}
