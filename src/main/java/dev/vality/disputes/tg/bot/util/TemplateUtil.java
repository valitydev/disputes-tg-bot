package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TemplateUtil {

    public static String prepareProviderCreateDisputeTemplate(ProviderChat providerChat, DisputeParams disputeParams,
                                                              String terminalId) {
        var template = providerChat.getTemplate();
        return template.replaceAll("\\$\\{dispute_id}", disputeParams.disputeId)
                .replaceAll("\\$\\{invoice_id}", disputeParams.getTransactionContext().getInvoiceId())
                .replaceAll("\\$\\{payment_id}", disputeParams.getTransactionContext().getPaymentId())
                .replaceAll("\\$\\{provider_trx_id}", disputeParams.getTransactionContext().getProviderTrxId())
                .replaceAll("\\$\\{amount}", FormatUtil.getFormattedAmount(disputeParams))
                .replaceAll("\\$\\{terminal_id}", terminalId)
                .replaceAll("\\$\\{payer_email}", FormatUtil.formatOptional(disputeParams.getPayerEmail().orElse(null)))
                .replaceAll("\\$\\{risk_score}", FormatUtil.formatOptional(disputeParams.getRiskScore().orElse(null)));
    }

    public static String prepareMerchantStatusDisputeTemplate(String template, MerchantDispute dispute) {
        return template.replace("${dispute_id}", dispute.getId().toString())
                .replace("${invoice_id}", dispute.getInvoiceId())
                .replace("${payment_id}", dispute.getPaymentId())
                .replace("${external_id}", FormatUtil.formatOptional(dispute.getExternalId()))
                .replace("${status}", dispute.getStatus().getLiteral())
                .replace("${message}", FormatUtil.formatOptional(dispute.getMessage()))
                .replace("${changed_amount}", FormatUtil.formatOptional(
                        dispute.getChangedAmount() == null
                                ? null
                                : FormatUtil.getFormattedAmount(dispute.getChangedAmount())));
    }

}
