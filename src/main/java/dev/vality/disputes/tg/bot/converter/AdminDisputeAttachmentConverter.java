package dev.vality.disputes.tg.bot.converter;

import dev.vality.disputes.admin.Attachment;
import dev.vality.disputes.tg.bot.util.FileUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Component
public class AdminDisputeAttachmentConverter {

    private static final String DEFAULT_EXTENSION = "bin";
    private static final Tika TIKA = new Tika();

    public InputFile convert(UUID disputeId, Attachment attachment) throws MimeTypeException {
        byte[] content = attachment.getData();
        String extension = detectExtension(content);
        String filename = FileUtil.createFileName(disputeId, extension);
        return TelegramUtil.convertToInputFile(content, filename);
    }

    private String detectExtension(byte[] content) throws MimeTypeException {
        var mimeType = MimeTypes.getDefaultMimeTypes().forName(TIKA.detect(content));
        String extension = mimeType.getExtension();
        if (extension == null || extension.isBlank()) {
            return DEFAULT_EXTENSION;
        }
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }
}
