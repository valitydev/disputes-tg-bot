package dev.vality.disputes.tg.bot.provider.dto;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.dto.File;
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
