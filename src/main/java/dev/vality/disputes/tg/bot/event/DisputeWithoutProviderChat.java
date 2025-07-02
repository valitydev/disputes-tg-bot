package dev.vality.disputes.tg.bot.event;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.disputes.provider.DisputeParams;
import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@Getter
@Builder
public class DisputeWithoutProviderChat {
    private ProviderRef providerRef;
    private DisputeParams disputeParams;
    private InputFile file;
}
