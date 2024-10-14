package dev.vality.disputes.tg.bot.provider.handler.event.inner;

import dev.vality.disputes.tg.bot.core.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.core.handler.InternalEventHandler;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDisputeInfoHandler implements InternalEventHandler<DisputeInfoDto> {

    private final ProviderDisputeDao providerDisputeDao;

    @EventListener
    @Override
    public void handle(DisputeInfoDto event) {
        log.info("Updating provider dispute info: {}", event);
        providerDisputeDao.updateReason(event.getInvoiceId(), event.getPaymentId(), event.getResponseText());
        log.info("Successfully updated provider dispute info: {}", event);
    }
}
