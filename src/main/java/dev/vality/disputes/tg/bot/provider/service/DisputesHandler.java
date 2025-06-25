package dev.vality.disputes.tg.bot.provider.service;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.disputes.provider.*;
import dev.vality.disputes.tg.bot.core.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.core.event.DisputeWithoutProviderChat;
import dev.vality.disputes.tg.bot.core.service.HellgateService;
import dev.vality.disputes.tg.bot.core.util.InvoiceUtil;
import dev.vality.disputes.tg.bot.provider.exception.DisputeCreationException;
import dev.vality.disputes.tg.bot.provider.exception.NotSupportedOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesHandler implements ProviderDisputesServiceSrv.Iface {

    private final HellgateService hellgateService;
    private final ProviderChatDao chatDao;
    private final DisputeCreationService disputeCreationService;
    private final DisputeAttachmentService attachmentService;
    private final TelegramNotificationService notificationService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public DisputeCreatedResult createDispute(DisputeParams disputeParams) {
        try {
            log.info("Received createDispute request: {}", disputeParams);
            var trxContext = disputeParams.getTransactionContext();
            var invoice = hellgateService.getInvoice(trxContext.getInvoiceId());
            var providerRef = InvoiceUtil.getProviderRef(invoice, trxContext.getPaymentId());
            var providerChat = chatDao.getByProviderId(providerRef.getId());
            if (providerChat.isEmpty()) {
                return createDisputeWithoutProviderChat(disputeParams, providerRef);
            }
            return createDisputeWithProviderChat(disputeParams, providerChat.get());
        } catch (Exception e) {
            throw new DisputeCreationException("Unable to create dispute, check stacktrace and previous logs", e);
        }
    }

    private DisputeCreatedResult createDisputeWithoutProviderChat(DisputeParams disputeParams,
                                                                 ProviderRef providerRef) {
        UUID disputeId = disputeCreationService.createProviderDispute(disputeParams);
        InputFile file = attachmentService.prepareDisputeAttachment(disputeId, disputeParams);
        log.info("Provider chat not found for dispute: {}", disputeId);
        var event = DisputeWithoutProviderChat.builder()
                .disputeParams(disputeParams)
                .providerRef(providerRef)
                .file(file)
                .build();
        events.publishEvent(event);
        return createSuccessResult(disputeParams.getDisputeId().get());
    }

    private DisputeCreatedResult createDisputeWithProviderChat(DisputeParams disputeParams, ProviderChat chat) 
            throws TelegramApiException {
        UUID disputeId = disputeCreationService.createProviderDispute(disputeParams, chat);
        InputFile file = attachmentService.prepareDisputeAttachment(disputeId, disputeParams);
        notificationService.sendDisputeNotification(chat, disputeParams, file, disputeId);
        return createSuccessResult(disputeId.toString());
    }

    private DisputeCreatedResult createSuccessResult(String providerDisputeId) {
        DisputeCreatedResult result = new DisputeCreatedResult();
        result.setSuccessResult(new DisputeCreatedSuccessResult()
                .setProviderDisputeId(providerDisputeId));
        return result;
    }

    @Override
    public DisputeStatusResult checkDisputeStatus(DisputeContext disputeContext) {
        throw new NotSupportedOperationException("Dispute must be processed manually");
    }
}