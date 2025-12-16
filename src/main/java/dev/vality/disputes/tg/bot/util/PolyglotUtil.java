package dev.vality.disputes.tg.bot.util;

import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.service.Polyglot;
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

    private static String formatStatusMessage(DisputeStatus status) {
        return switch (status) {
            case pending -> String.format("%s ⏳", status.getLiteral());
            case approved -> String.format("%s ✅", status.getLiteral());
            case declined -> String.format("%s ❌", status.getLiteral());
        };
    }

    private static MessageType determineMessageType(MerchantDispute dispute) {
        if (dispute.getMessage() != null && dispute.getChangedAmount() != null && dispute.getExternalId() != null) {
            return MessageType.STATUS_WITH_AMOUNT_EXTERNAL_ID_MESSAGE;
        }
        if (dispute.getMessage() != null && dispute.getChangedAmount() != null && dispute.getExternalId() == null) {
            return MessageType.STATUS_WITH_AMOUNT_MESSAGE;
        }
        if (dispute.getMessage() != null && dispute.getChangedAmount() == null && dispute.getExternalId() != null) {
            return MessageType.STATUS_WITH_EXTERNAL_ID_MESSAGE;
        }
        if (dispute.getMessage() == null && dispute.getChangedAmount() != null && dispute.getExternalId() != null) {
            return MessageType.STATUS_WITH_AMOUNT_EXTERNAL_ID;
        }
        if (dispute.getMessage() != null) {
            return MessageType.STATUS_WITH_MESSAGE;
        }
        if (dispute.getChangedAmount() != null) {
            return MessageType.STATUS_WITH_AMOUNT;
        }
        if (dispute.getExternalId() != null) {
            return MessageType.STATUS_WITH_EXTERNAL_ID;
        }
        return MessageType.STATUS;
    }

    private static String buildStatusMessage(MerchantDispute dispute, String statusMessage, MessageType messageType, 
                                           Locale replyLocale, Polyglot polyglot) {
        return switch (messageType) {
            case STATUS -> polyglot.getText(replyLocale, "dispute.status",
                    dispute.getInvoiceId(), statusMessage);
            case STATUS_WITH_AMOUNT -> polyglot.getText(replyLocale, "dispute.status-amount",
                    dispute.getInvoiceId(), statusMessage, FormatUtil.getFormattedAmount(dispute.getChangedAmount()));
            case STATUS_WITH_EXTERNAL_ID -> polyglot.getText(replyLocale, "dispute.status-ext-id",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage);
            case STATUS_WITH_MESSAGE -> polyglot.getText(replyLocale, "dispute.status-message",
                    dispute.getInvoiceId(), statusMessage, dispute.getMessage());
            case STATUS_WITH_AMOUNT_EXTERNAL_ID -> polyglot.getText(replyLocale, "dispute.status-ext-id-amount",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage,
                    FormatUtil.getFormattedAmount(dispute.getChangedAmount()));
            case STATUS_WITH_AMOUNT_MESSAGE -> polyglot.getText(replyLocale, "dispute.status-amount-message",
                    dispute.getInvoiceId(), statusMessage, FormatUtil.getFormattedAmount(dispute.getChangedAmount()),
                    dispute.getMessage());
            case STATUS_WITH_EXTERNAL_ID_MESSAGE -> polyglot.getText(replyLocale, "dispute.status-ext-id-message",
                    dispute.getInvoiceId(), dispute.getExternalId(), statusMessage, dispute.getMessage());
            case STATUS_WITH_AMOUNT_EXTERNAL_ID_MESSAGE ->
                    polyglot.getText(replyLocale, "dispute.status-ext-id-amount-message",
                            dispute.getInvoiceId(), dispute.getExternalId(), statusMessage,
                            FormatUtil.getFormattedAmount(dispute.getChangedAmount()), dispute.getMessage());
        };
    }

    private enum MessageType {
        STATUS,
        STATUS_WITH_AMOUNT,
        STATUS_WITH_EXTERNAL_ID,
        STATUS_WITH_MESSAGE,
        STATUS_WITH_AMOUNT_EXTERNAL_ID,
        STATUS_WITH_AMOUNT_MESSAGE,
        STATUS_WITH_EXTERNAL_ID_MESSAGE,
        STATUS_WITH_AMOUNT_EXTERNAL_ID_MESSAGE
    }

    private static String formatAmount(long amount) {
        //TODO: Get currency from disputes-api?
        return new BigDecimal(amount).movePointLeft(2).stripTrailingZeros().toPlainString();
    }

    public static String explanation(Polyglot polyglot, String errorCode) {
        try {
            return polyglot.getText(errorCode);
        } catch (Exception e) {
            log.info("Unable to find error description with code '{}'", errorCode);
            return errorCode;
        }
    }
}
