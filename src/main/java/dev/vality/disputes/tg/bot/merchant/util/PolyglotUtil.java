package dev.vality.disputes.tg.bot.merchant.util;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@UtilityClass
public class PolyglotUtil {

    public static String prepareStatusMessage(MerchantDispute dispute, Locale replyLocale, Polyglot polyglot) {
        var statusMessage = formatStatusMessage(dispute.getStatus());
        var messageType = determineMessageType(dispute);
        
        return buildStatusMessage(dispute, statusMessage, messageType, replyLocale, polyglot);
    }

    private static String formatStatusMessage(dev.vality.disputes.tg.bot.core.domain.enums.DisputeStatus status) {
        return switch (status) {
            case pending -> String.format("%s ⏳", status.getLiteral());
            case approved -> String.format("%s ✅", status.getLiteral());
            case declined -> String.format("%s ❌", status.getLiteral());
        };
    }

    private static MessageType determineMessageType(MerchantDispute dispute) {
        if (dispute.getMessage() == null) {
            return dispute.getExternalId() == null ? MessageType.STATUS_WO_EXTERNAL_ID : MessageType.STATUS;
        }
        if (dispute.getChangedAmount() != null) {
            return dispute.getExternalId() == null ? MessageType.STATUS_WITH_AMOUNT_WO_EXTERNAL_ID :
                    MessageType.STATUS_WITH_AMOUNT;
        }
        return dispute.getExternalId() == null ? MessageType.STATUS_WITH_MESSAGE_WO_EXTERNAL_ID :
                MessageType.STATUS_WITH_MESSAGE;
    }

    private static String buildStatusMessage(MerchantDispute dispute, String statusMessage, MessageType messageType, 
                                           Locale replyLocale, Polyglot polyglot) {
        return switch (messageType) {
            case STATUS_WO_EXTERNAL_ID -> polyglot.getText(replyLocale, "dispute.status-wo-external-id",
                    dispute.getInvoiceId(), statusMessage);
            case STATUS -> polyglot.getText(replyLocale, "dispute.status",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage);
            case STATUS_WITH_AMOUNT_WO_EXTERNAL_ID ->
                    polyglot.getText(replyLocale, "dispute.status-with-amount-wo-external-id",
                            dispute.getInvoiceId(), statusMessage, formatAmount(dispute.getChangedAmount()));
            case STATUS_WITH_AMOUNT -> polyglot.getText(replyLocale, "dispute.status-with-amount",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage,
                    formatAmount(dispute.getChangedAmount()));
            case STATUS_WITH_MESSAGE_WO_EXTERNAL_ID ->
                    polyglot.getText(replyLocale, "dispute.status-with-message-wo-external-id",
                            dispute.getInvoiceId(), statusMessage, dispute.getMessage());
            case STATUS_WITH_MESSAGE -> polyglot.getText(replyLocale, "dispute.status-with-message",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage, dispute.getMessage());
        };
    }

    private enum MessageType {
        STATUS_WO_EXTERNAL_ID,
        STATUS,
        STATUS_WITH_AMOUNT_WO_EXTERNAL_ID,
        STATUS_WITH_AMOUNT,
        STATUS_WITH_MESSAGE_WO_EXTERNAL_ID,
        STATUS_WITH_MESSAGE
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
