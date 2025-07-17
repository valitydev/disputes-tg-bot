package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TemplateUtil {

    public static String prepareTemplate(ProviderChat providerChat, DisputeParams disputeParams, String terminalId) {
        var template = providerChat.getTemplate();
        return template.replaceAll("\\$\\{dispute_id}", disputeParams.disputeId)
                .replaceAll("\\$\\{invoice_id}", disputeParams.getTransactionContext().getInvoiceId())
                .replaceAll("\\$\\{payment_id}", disputeParams.getTransactionContext().getPaymentId())
                .replaceAll("\\$\\{provider_trx_id}", disputeParams.getTransactionContext().getProviderTrxId())
                .replaceAll("\\$\\{amount}", FormatUtil.getFormattedAmount(disputeParams))
                .replaceAll("\\$\\{terminal_id}",terminalId);
    }

} 