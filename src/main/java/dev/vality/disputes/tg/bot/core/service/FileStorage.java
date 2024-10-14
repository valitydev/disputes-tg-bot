package dev.vality.disputes.tg.bot.core.service;

import dev.vality.disputes.provider.Attachment;
import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.provider.dto.CreateDisputeDto;
import dev.vality.disputes.tg.bot.provider.exception.BadRequestException;
import dev.vality.disputes.tg.bot.provider.exception.FileStorageException;
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

    public CreateDisputeDto.Document processAttachment(DisputeParams disputeParams) {
        var attachment = validateAndGetAttachment(disputeParams);
        byte[] content;
        try {
            content = downloadAttachmentContent(attachment);
            log.info("Attachment downloaded successfully ({}kb)", content.length / 1024);
        } catch (IOException e) {
            throw new FileStorageException("Unable to download file from file storage", e);
        }
        return convertToDocument(attachment, content);
    }

    private static CreateDisputeDto.Document convertToDocument(Attachment attachment, byte[] content) {
        MediaType mediaType = MediaType.parseMediaType(attachment.getMimeType());
        var document = new CreateDisputeDto.Document();
        document.setContent(content);
        document.setMediaType(mediaType);
        return document;
    }

    private byte[] downloadAttachmentContent(Attachment attachment) throws IOException {
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

    private Attachment validateAndGetAttachment(DisputeParams disputeParams) {
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
