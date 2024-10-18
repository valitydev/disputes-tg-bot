package dev.vality.disputes.tg.bot.merchant.util;

import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PolyglotUtil {

    public static String prepareStatusMessage(MerchantDispute dispute, String replyLocale, Polyglot polyglot) {
        var statusMessage = switch (dispute.getStatus()) {
            case pending -> dispute.getStatus().getLiteral() + " ❌";
            case approved -> dispute.getStatus().getLiteral() + " ✅";
            case declined -> dispute.getStatus().getLiteral() + " ⏳";
        };
        return polyglot.getText(replyLocale, "dispute.status",
                dispute.getDisputeId(),
                dispute.getInvoiceId(),
                dispute.getExternalId(),
                statusMessage,
                dispute.getUpdatedAt());
    }
}
