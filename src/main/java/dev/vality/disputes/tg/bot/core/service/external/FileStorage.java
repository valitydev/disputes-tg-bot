package dev.vality.disputes.tg.bot.core.service.external;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.dto.File;
import dev.vality.disputes.tg.bot.core.exception.BadRequestException;
import dev.vality.disputes.tg.bot.core.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorage {

    private final HttpClient s3HttpClient;

    public File processFile(DisputeParams disputeParams) {
        var attachment = validateAndGetAttachment(disputeParams);
        byte[] content;
        try {
            content = downloadAttachmentContent(attachment);
            log.info("Attachment downloaded successfully ({}kb)", content.length / 1024);
        } catch (IOException e) {
            throw new FileStorageException("Unable to download file from file storage", e);
        }
        return convertToFile(attachment, content);
    }

    private static File convertToFile(dev.vality.disputes.provider.Attachment attachment, byte[] content) {
        MediaType mediaType = MediaType.parseMediaType(attachment.getMimeType());
        var document = new File();
        document.setContent(content);
        document.setMediaType(mediaType);
        return document;
    }

    private byte[] downloadAttachmentContent(dev.vality.disputes.provider.Attachment attachment) throws IOException {
        log.info("Downloading attachment: {}", attachment);
        return s3HttpClient.execute(
                new HttpGet(attachment.getSourceUrl()),
                new AbstractHttpClientResponseHandler<>() {
                    @Override
                    public byte[] handleEntity(HttpEntity entity) throws IOException {
                        return EntityUtils.toByteArray(entity);
                    }
                });
    }

    private dev.vality.disputes.provider.Attachment validateAndGetAttachment(DisputeParams disputeParams) {
        log.info("Trying to download data from s3 {}", disputeParams);
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
