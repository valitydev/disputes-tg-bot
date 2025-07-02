package dev.vality.disputes.tg.bot.service.external;

import dev.vality.disputes.merchant.*;
import dev.vality.disputes.tg.bot.exception.*;
import dev.vality.disputes.tg.bot.util.ExceptionUtil;
import dev.vality.woody.api.flow.error.WRuntimeException;
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
        } catch (WRuntimeException we) {
            if (ExceptionUtil.isTrxInfoMissingException(we)) {
                throw new PaymentNotStartedException();
            }
            if (ExceptionUtil.isCapturedPaymentException(we)) {
                throw new PaymentCapturedException();
            }
            if (ExceptionUtil.isInvoicingPaymentStatusRestrictionsException(we)) {
                throw new PaymentStatusRestrictionException();
            }
            if (ExceptionUtil.isUnexpectedMimeTypeException(we)) {
                throw new AttachmentTypeNotSupportedException();
            }
            throw new DisputesApiException("Failed to create dispute", we);
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
        } catch (TException e1) {
            throw new DisputesApiException("Failed to check dispute status", e1);
        }
    }
}
