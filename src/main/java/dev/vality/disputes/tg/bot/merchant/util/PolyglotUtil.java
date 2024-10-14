package dev.vality.disputes.tg.bot.merchant.util;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@UtilityClass
public class PolyglotUtil {

    public static String prepareStatusMessage(MerchantDispute dispute, String replyLocale, Polyglot polyglot) {
        var statusMessage = switch (dispute.getStatus()) {
            case pending -> String.format("%s ⏳", dispute.getStatus().getLiteral());
            case approved -> String.format("%s ✅", dispute.getStatus().getLiteral());
            case declined -> String.format("%s ❌", dispute.getStatus().getLiteral());
        };

        if (dispute.getMessage() == null) {
            return polyglot.getText(replyLocale, "dispute.status",
                    dispute.getInvoiceId(),
                    dispute.getExternalId(),
                    statusMessage);
        }
        if (dispute.getChangedAmount() != null) {
            return polyglot.getText(replyLocale, "dispute.status-with-amount",
                    dispute.getInvoiceId(),
                    dispute.getExternalId(),
                    statusMessage,
                    formatAmount(dispute.getChangedAmount())
            );
        }
        return polyglot.getText(replyLocale, "dispute.status-with-message",
                dispute.getInvoiceId(),
                dispute.getExternalId(),
                statusMessage,
                dispute.getMessage());

    }

    private static String formatAmount(long amount) {
        //TODO: Get currency from disputes-api?
        return new BigDecimal(amount).movePointLeft(2).stripTrailingZeros().toPlainString();
    }

    public static String explanation(Polyglot polyglot, String errorCode) {
        try {
            return polyglot.getText(errorCode);
        } catch (Exception e) {
            log.warn("Unable to find error description with code '{}'", errorCode);
            return errorCode;
        }
    }
}
