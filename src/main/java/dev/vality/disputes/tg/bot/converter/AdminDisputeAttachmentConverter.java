package dev.vality.disputes.tg.bot.converter;

import dev.vality.disputes.admin.AttachmentRef;
import dev.vality.disputes.tg.bot.service.external.FileStorageService;
import dev.vality.disputes.tg.bot.util.FileUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminDisputeAttachmentConverter {

    private static final String DEFAULT_EXTENSION = "bin";
    private final FileStorageService fileStorageService;

    public InputFile convert(UUID disputeId, AttachmentRef attachmentRef) throws MimeTypeException {
        var file = fileStorageService.downloadAdminAttachment(attachmentRef);
        String extension = detectExtension(attachmentRef.getMimeType());
        String filename = FileUtil.createFileName(disputeId, extension);
        return TelegramUtil.convertToInputFile(file.getContent(), filename);
    }

    private String detectExtension(String contentType) throws MimeTypeException {
        var mimeType = MimeTypes.getDefaultMimeTypes().forName(contentType);
        String extension = mimeType.getExtension();
        if (extension == null || extension.isBlank()) {
            return DEFAULT_EXTENSION;
        }
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }
}
