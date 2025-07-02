package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeCreationService {

    private final ProviderDisputeDao providerDisputeDao;

    @Transactional
    public UUID createProviderDispute(DisputeParams disputeParams) {
        return createProviderDispute(disputeParams, null);
    }

    @Transactional
    public UUID createProviderDispute(DisputeParams disputeParams, ProviderChat chat) {
        ProviderDispute providerDispute = buildProviderDispute(disputeParams, chat);
        UUID disputeId = providerDisputeDao.save(providerDispute);
        log.debug("Created dispute id: {}", disputeId);
        return disputeId;
    }

    private ProviderDispute buildProviderDispute(DisputeParams disputeParams) {
        ProviderDispute providerDispute = new ProviderDispute();
        if (disputeParams.getDisputeId().isPresent()) {
            providerDispute.setId(UUID.fromString(disputeParams.getDisputeId().get()));
        }
        providerDispute.setCreatedAt(LocalDateTime.now());
        providerDispute.setInvoiceId(disputeParams.getTransactionContext().getInvoiceId());
        providerDispute.setPaymentId(disputeParams.getTransactionContext().getPaymentId());
        providerDispute.setProviderTrxId(disputeParams.getTransactionContext().getProviderTrxId());
        return providerDispute;
    }

    private ProviderDispute buildProviderDispute(DisputeParams disputeParams, ProviderChat chat) {
        ProviderDispute providerDispute = buildProviderDispute(disputeParams);
        if (chat != null) {
            providerDispute.setChatId(chat.getId());
        }
        return providerDispute;
    }
} 