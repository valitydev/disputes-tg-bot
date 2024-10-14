package dev.vality.disputes.tg.bot.merchant.service.external;

import dev.vality.disputes.merchant.*;
import dev.vality.disputes.tg.bot.common.exception.DisputesApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesApiService {

    private final MerchantDisputesServiceSrv.Iface disputesMerchantClient;

    public DisputeCreatedResult createDispute(DisputeParams disputeParams) {
        try {
            log.debug("CreateDispute for invoice_id '{}', payment_id '{}' and {} attachments",
                    disputeParams.getInvoiceId(), disputeParams.getPaymentId(), disputeParams.getAttachmentsSize());
            var result = disputesMerchantClient.createDispute(disputeParams);
            log.debug("CreateDispute for invoice_id '{}' and payment_id '{}' processed with result '{}'",
                    disputeParams.getInvoiceId(), disputeParams.getPaymentId(), result.getSetField().getFieldName());
            return result;
        } catch (Exception e) {
            throw new DisputesApiException("Failed to create dispute", e);
        }
    }

    public DisputeStatusResult getStatus(DisputeContext disputeContext) throws DisputeNotFound {
        try {
            log.debug("CheckDisputeStatus for dispute_id '{}'", disputeContext.getDisputeId());
            var result = disputesMerchantClient.checkDisputeStatus(disputeContext);
            log.debug("CheckDisputeStatus for dispute_id '{}' processed with result '{}'",
                    disputeContext.getDisputeId(),
                    result.getSetField().getFieldName());
            return result;
        } catch (DisputeNotFound e) {
            throw e;
        } catch (TException e1) {
            throw new DisputesApiException("Failed to check dispute status", e1);
        }
    }
}
