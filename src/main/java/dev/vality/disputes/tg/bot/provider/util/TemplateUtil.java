package dev.vality.disputes.tg.bot.provider.util;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.util.FormatUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TemplateUtil {

    public static String prepareTemplate(String template, DisputeParams disputeParams) {
        return template.replaceAll("\\$\\{dispute_id}", disputeParams.disputeId)
                .replaceAll("\\$\\{invoice_id}", disputeParams.getTransactionContext().getInvoiceId())
                .replaceAll("\\$\\{payment_id}", disputeParams.getTransactionContext().getPaymentId())
                .replaceAll("\\$\\{provider_trx_id}", disputeParams.getTransactionContext().getProviderTrxId())
                .replaceAll("\\$\\{amount_formatted}", FormatUtil.getFormattedAmount(disputeParams));
    }

} 