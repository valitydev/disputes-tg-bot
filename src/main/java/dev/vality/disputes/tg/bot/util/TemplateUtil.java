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
        return template.replace("${dispute_id}", escape(disputeParams.disputeId))
                .replace("${invoice_id}", escape(disputeParams.getTransactionContext().getInvoiceId()))
                .replace("${payment_id}", escape(disputeParams.getTransactionContext().getPaymentId()))
                .replace("${provider_trx_id}", escape(disputeParams.getTransactionContext().getProviderTrxId()))
                .replace("${amount}", escape(FormatUtil.getFormattedAmount(disputeParams)))
                .replace("${terminal_id}", escape(terminalId))
                .replace("${payer_email}",
                        escape(FormatUtil.formatOptional(disputeParams.getPayerEmail().orElse(null))))
                .replace("${risk_score}", escape(FormatUtil.formatOptional(disputeParams.getRiskScore().orElse(null))));
    }

    public static String prepareMerchantStatusDisputeTemplate(String template, MerchantDispute dispute) {
        return template.replace("${dispute_id}", escape(dispute.getId().toString()))
                .replace("${invoice_id}", escape(dispute.getInvoiceId()))
                .replace("${payment_id}", escape(dispute.getPaymentId()))
                .replace("${external_id}", escape(FormatUtil.formatOptional(dispute.getExternalId())))
                .replace("${status}", escape(dispute.getStatus().getLiteral()))
                .replace("${message}", escape(FormatUtil.formatOptional(dispute.getMessage())))
                .replace("${changed_amount}", FormatUtil.formatOptional(
                        dispute.getChangedAmount() == null
                                ? null
                                : escape(FormatUtil.getFormattedAmount(dispute.getChangedAmount()))));
    }

    private static String escape(String text) {
        return TelegramUtil.escapeHtml(text);
    }

}
