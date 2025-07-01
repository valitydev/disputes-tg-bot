package dev.vality.disputes.tg.bot.merchant.service.external;

import dev.vality.bender.BenderSrv;
import dev.vality.bender.GetInternalIDResult;
import dev.vality.disputes.tg.bot.merchant.exception.BenderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenderService {

    private static final String BENDER_PATH_FORMAT = "capi/CreateInvoice/%s/%s";

    private final BenderSrv.Iface benderClient;

    public String getInvoiceId(String partyId, String externalId) {
        String benderPath = String.format(BENDER_PATH_FORMAT, partyId, externalId);
        log.debug("Getting invoice ID via Bender with path: {}", benderPath);
        return getInvoiceId(benderPath);
    }

    private String getInvoiceId(String externalId) {
        try {
            log.debug("Getting internal id from bender, externalId: {}", externalId);
            GetInternalIDResult result = benderClient.getInternalID(externalId);
            log.debug("Received internal id {} from bender, externalId: {}", result.getInternalId(), externalId);
            return result.getInternalId();
        } catch (TException e) {
            throw new BenderException(String.format("Failed to get internal id with externalId: %s", externalId), e);
        }
    }
}
