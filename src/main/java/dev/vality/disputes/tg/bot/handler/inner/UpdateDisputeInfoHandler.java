package dev.vality.disputes.tg.bot.handler.inner;

import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.handler.InternalEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDisputeInfoHandler implements InternalEventHandler<DisputeInfoDto> {

    private final ProviderDisputeDao providerDisputeDao;

    @TransactionalEventListener
    @Override
    public void handle(DisputeInfoDto event) {
        log.info("Updating provider dispute info: {}", event);
        providerDisputeDao.updateReason(event.getInvoiceId(), event.getPaymentId(), event.getResponseText());
        log.info("Successfully updated provider dispute info: {}", event);
    }
}
