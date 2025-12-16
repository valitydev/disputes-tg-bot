package dev.vality.disputes.tg.bot.util;

import dev.vality.woody.api.flow.error.WErrorSource;
import dev.vality.woody.api.flow.error.WErrorType;
import dev.vality.woody.api.flow.error.WRuntimeException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionUtil {

    public static boolean isTrxInfoMissingException(WRuntimeException exception) {
        return isSpecificException(exception, "NotFoundException");
    }

    public static boolean isCapturedPaymentException(WRuntimeException exception) {
        return isSpecificException(exception, "CapturedPaymentException");
    }

    public static boolean isInvoicingPaymentStatusRestrictionsException(WRuntimeException exception) {
        return isSpecificException(exception, "InvoicingPaymentStatusRestrictionsException");
    }

    public static boolean isUnexpectedMimeTypeException(WRuntimeException exception) {
        return isSpecificException(exception, "UnexpectedMimeTypeException");
    }

    public static boolean isPaymentExpiredException(WRuntimeException exception) {
        return isSpecificException(exception, "PaymentExpiredException");
    }

    private static boolean isSpecificException(WRuntimeException exception, String exceptionName) {
        return hasValidErrorDefinition(exception) 
                && exception.getErrorDefinition().getErrorReason().contains(exceptionName);
    }

    private static boolean hasValidErrorDefinition(WRuntimeException exception) {
        return exception.getErrorDefinition() != null
                && exception.getErrorDefinition().getGenerationSource() == WErrorSource.EXTERNAL
                && exception.getErrorDefinition().getErrorType() == WErrorType.UNEXPECTED_ERROR
                && exception.getErrorDefinition().getErrorSource() == WErrorSource.INTERNAL
                && exception.getErrorDefinition().getErrorReason() != null;
    }
}
