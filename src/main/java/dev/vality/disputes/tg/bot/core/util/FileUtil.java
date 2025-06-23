package dev.vality.disputes.tg.bot.core.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class FileUtil {

    public static String createFileName(UUID disputeId, String extension) {
        return disputeId.toString() + "." + extension;
    }
}
