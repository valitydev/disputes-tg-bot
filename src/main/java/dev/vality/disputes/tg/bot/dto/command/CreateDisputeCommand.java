package dev.vality.disputes.tg.bot.dto.command;

import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CreateDisputeCommand extends BaseCommand {

    private final DisputeInfoDto disputeInfo;
} 