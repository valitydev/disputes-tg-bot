package dev.vality.disputes.tg.bot.handler.admin.event.internal;

import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.event.DisputeWithoutProviderChat;
import dev.vality.disputes.tg.bot.handler.InternalEventHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.buildTextWithAttachmentResponse;

@Component
@RequiredArgsConstructor
public class CreateDisputeWithoutChatHandler implements InternalEventHandler<DisputeWithoutProviderChat> {

    private final DominantService dominantCacheService;
    private final AdminChatProperties adminChatProperties;
    private final Polyglot polyglot;
    private final TelegramClient telegramClient;

    @Override
    @SneakyThrows
    @TransactionalEventListener
    public void handle(DisputeWithoutProviderChat event) {
        var provider = dominantCacheService.getProvider(event.getProviderRef());
        var disputeParams = event.getDisputeParams();
        var trxContext = disputeParams.getTransactionContext();
        String text = polyglot.getText("dispute.provider.created-wo-chat",
                event.getProviderRef().getId(), provider.getName(),
                trxContext.getProviderTrxId(),
                trxContext.getInvoiceId() + "." + trxContext.getPaymentId(),
                disputeParams.getDisputeId().get());
        var request = buildTextWithAttachmentResponse(adminChatProperties.getId(), text, event.getFile());
        request.setMessageThreadId(adminChatProperties.getTopics().getUnknownProvider());
        telegramClient.execute(request);
    }
}
