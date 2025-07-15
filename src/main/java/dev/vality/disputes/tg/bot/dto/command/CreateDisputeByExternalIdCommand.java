package dev.vality.disputes.tg.bot.dto.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CreateDisputeByExternalIdCommand extends BaseCommand {

    private final String externalId;
} 