package dev.vality.disputes.tg.bot.core.util;

import dev.vality.woody.api.flow.error.WErrorSource;
import dev.vality.woody.api.flow.error.WErrorType;
import dev.vality.woody.api.flow.error.WRuntimeException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionUtil {

    public static boolean isTrxInfoMissingException(WRuntimeException exception) {
        return exception.getErrorDefinition() != null
                && exception.getErrorDefinition().getGenerationSource() == WErrorSource.EXTERNAL
                && exception.getErrorDefinition().getErrorType() == WErrorType.UNEXPECTED_ERROR
                && exception.getErrorDefinition().getErrorSource() == WErrorSource.INTERNAL
                && exception.getErrorDefinition().getErrorReason() != null
                && exception.getErrorDefinition().getErrorReason().contains("NotFoundException");
    }

    public static boolean isCapturedPaymentException(WRuntimeException exception) {
        return exception.getErrorDefinition() != null
                && exception.getErrorDefinition().getGenerationSource() == WErrorSource.EXTERNAL
                && exception.getErrorDefinition().getErrorType() == WErrorType.UNEXPECTED_ERROR
                && exception.getErrorDefinition().getErrorSource() == WErrorSource.INTERNAL
                && exception.getErrorDefinition().getErrorReason() != null
                && exception.getErrorDefinition().getErrorReason().contains("CapturedPaymentException");
    }

    public static boolean isInvoicingPaymentStatusRestrictionsException(WRuntimeException exception) {
        return exception.getErrorDefinition() != null
                && exception.getErrorDefinition().getGenerationSource() == WErrorSource.EXTERNAL
                && exception.getErrorDefinition().getErrorType() == WErrorType.UNEXPECTED_ERROR
                && exception.getErrorDefinition().getErrorSource() == WErrorSource.INTERNAL
                && exception.getErrorDefinition().getErrorReason() != null
                && exception.getErrorDefinition().getErrorReason().contains(
                        "InvoicingPaymentStatusRestrictionsException");
    }

    public static boolean isUnexpectedMimeTypeException(WRuntimeException exception) {
        return exception.getErrorDefinition() != null
                && exception.getErrorDefinition().getGenerationSource() == WErrorSource.EXTERNAL
                && exception.getErrorDefinition().getErrorType() == WErrorType.UNEXPECTED_ERROR
                && exception.getErrorDefinition().getErrorSource() == WErrorSource.INTERNAL
                && exception.getErrorDefinition().getErrorReason() != null
                && exception.getErrorDefinition().getErrorReason().contains("UnexpectedMimeTypeException");
    }
}
