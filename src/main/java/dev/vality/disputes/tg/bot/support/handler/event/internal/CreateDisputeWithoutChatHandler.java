package dev.vality.disputes.tg.bot.support.handler.event.internal;

import dev.vality.disputes.tg.bot.core.event.DisputeWithoutProviderChat;
import dev.vality.disputes.tg.bot.core.handler.InternalEventHandler;
import dev.vality.disputes.tg.bot.core.service.DominantCacheServiceImpl;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.buildTextWithAttachmentResponse;

@Component
@RequiredArgsConstructor
public class CreateDisputeWithoutChatHandler implements InternalEventHandler<DisputeWithoutProviderChat> {

    private final DominantCacheServiceImpl dominantCacheService;
    private final SupportChatProperties supportChatProperties;
    private final Polyglot polyglot;
    private final TelegramClient telegramClient;

    @Override
    @SneakyThrows
    @EventListener
    public void handle(DisputeWithoutProviderChat event) {
        var provider = dominantCacheService.getProvider(event.getProviderRef());
        var disputeParams = event.getDisputeParams();
        var trxContext = disputeParams.getTransactionContext();
        String text = polyglot.getText("dispute.provider.created-wo-chat",
                event.getProviderRef().getId(), provider.getName(),
                trxContext.getProviderTrxId(),
                trxContext.getInvoiceId() + "." + trxContext.getPaymentId(),
                disputeParams.getDisputeId().get());
        var request = buildTextWithAttachmentResponse(supportChatProperties.getId(), text, event.getFile());
        request.setMessageThreadId(supportChatProperties.getTopics().getUnknownProvider());
        telegramClient.execute(request);
    }
}
