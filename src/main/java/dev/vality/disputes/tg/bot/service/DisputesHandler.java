package dev.vality.disputes.tg.bot.service;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.disputes.provider.*;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.exception.DisputeCreationException;
import dev.vality.disputes.tg.bot.exception.NotSupportedOperationException;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import dev.vality.disputes.tg.bot.util.InvoiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesHandler implements ProviderDisputesServiceSrv.Iface {

    private final HellgateService hellgateService;
    private final ProviderChatDao chatDao;
    private final ProviderDisputeDao providerDisputeDao;
    private final DisputeCreationService disputeCreationService;
    private final AttachmentService attachmentService;
    private final TelegramNotificationService notificationService;
    private final DominantService dominantCacheService;
    private final AdminChatProperties adminChatProperties;
    private final Polyglot polyglot;
    private final TelegramApiService telegramApiService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public DisputeCreatedResult createDispute(DisputeParams disputeParams) {
        try {
            log.info("Received createDispute request: {}", disputeParams);
            var trxContext = disputeParams.getTransactionContext();
            
            // Check if dispute already exists (idempotency)
            if (disputeParams.getDisputeId().isPresent()) {
                var disputeId = UUID.fromString(disputeParams.getDisputeId().get());
                var existingDispute = providerDisputeDao.get(disputeId);
                if (existingDispute != null) {
                    log.info("Dispute already exists with id: {}", disputeId);
                    return createSuccessResult(existingDispute.getId().toString());
                }
            }
            
            var invoice = hellgateService.getInvoice(trxContext.getInvoiceId());
            var route = InvoiceUtil.getRoute(invoice, trxContext.getPaymentId());
            var providerChat = chatDao.getByProviderIdAndTerminalId(route.getProvider().getId(),
                    route.getTerminal().getId())
                    .or(() -> chatDao.getByProviderId(route.getProvider().getId()));
            return providerChat.map(chat -> createDisputeWithProviderChat(disputeParams, chat))
                    .orElseGet(() -> createDisputeWithoutProviderChat(disputeParams, route));
        } catch (Exception e) {
            throw new DisputeCreationException("Unable to create dispute, check stacktrace and previous logs", e);
        }
    }

    private DisputeCreatedResult createDisputeWithoutProviderChat(DisputeParams disputeParams,
                                                                 PaymentRoute paymentRoute) {
        UUID disputeId = disputeCreationService.createProviderDispute(disputeParams);
        InputFile file = attachmentService.prepareAttachment(disputeId, disputeParams);
        log.info("Provider chat not found for dispute: {}", disputeId);
        var provider = dominantCacheService.getProvider(paymentRoute.getProvider());
        var terminal = dominantCacheService.getTerminal(paymentRoute.getTerminal());
        var trxContext = disputeParams.getTransactionContext();
        String text = polyglot.getText("dispute.provider.created-wo-chat",
                paymentRoute.getProvider().getId(), provider.getName(),
                paymentRoute.getTerminal().getId(), terminal.getName(),
                trxContext.getProviderTrxId(),
                trxContext.getInvoiceId() + "." + trxContext.getPaymentId(),
                disputeParams.getDisputeId().orElseThrow());
        telegramApiService.sendMessageWithDocument(text, adminChatProperties.getId(),
                adminChatProperties.getTopics().getUnknownProvider(), file);
        return createSuccessResult(disputeParams.getDisputeId().get());
    }

    private DisputeCreatedResult createDisputeWithProviderChat(DisputeParams disputeParams, ProviderChat chat) {
        UUID disputeId = disputeCreationService.createProviderDispute(disputeParams, chat);
        InputFile file = attachmentService.prepareAttachment(disputeId, disputeParams);
        notificationService.sendDisputeNotificationToProvider(chat, disputeParams, file, disputeId);
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