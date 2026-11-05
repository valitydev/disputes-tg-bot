package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.exception.BadRequestException;
import dev.vality.disputes.tg.bot.service.external.FileStorageService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final FileStorageService fileStorageService;

    @SneakyThrows
    public InputFile prepareAttachment(UUID disputeId, DisputeParams disputeParams) {
        var attachment = validateAndGetAttachment(disputeParams);
        return prepareAttachment(disputeId.toString(), attachment.getSourceUrl(), attachment.getMimeType());
    }

    public InputFile prepareAttachment(String filenamePrefix, String sourceUrl, String mimeType)
            throws MimeTypeException {
        var content = fileStorageService.downloadFile(sourceUrl);
        String extension = MimeTypes.getDefaultMimeTypes()
                .forName(MediaType.parse(mimeType).getBaseType().toString()).getExtension();
        return TelegramUtil.convertToInputFile(content, filenamePrefix + extension);
    }

    private dev.vality.disputes.provider.Attachment validateAndGetAttachment(DisputeParams disputeParams) {
        if (disputeParams.getAttachmentsSize() == 0) {
            log.error("Unable to find attachments in request");
            throw new BadRequestException("Unable to find attachments");
        }
        if (disputeParams.getAttachmentsSize() > 1) {
            log.warn("Too many attachments in request ({}), only first one will be send to provider, due to telegram " +
                            "restrictions",
                    disputeParams.getAttachmentsSize());
        }
        return disputeParams.getAttachments().getFirst();
    }
}
