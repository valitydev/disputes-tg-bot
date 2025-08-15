package dev.vality.disputes.tg.bot.converter;

import dev.vality.disputes.merchant.Attachment;
import dev.vality.disputes.tg.bot.exception.UnexpectedException;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateToAttachmentConverter {

    private static final String DEFAULT_COMPRESSED_PHOTO_MIME_TYPE = "image/jpeg";
    private static final Tika tika = new Tika();
    private final TelegramApiService telegramApiService;

    public Attachment convert(Update update) throws TelegramApiException, IOException {
        String fileId;
        String mimeType;
        var optionalDocument = getDocument(update);
        var optionalPhoto = getPhoto(update);
        var optionalVideo = getVideo(update);

        if (optionalDocument.isPresent()) {
            fileId = optionalDocument.get().getFileId();
            mimeType = optionalDocument.get().getMimeType();
        } else if (optionalPhoto.isPresent()) {
            // If no fileId present, throw exception, cause this condition must be prevented on earlier stages.
            fileId = optionalPhoto.map(PhotoSize::getFileId).orElseThrow();
            // Telegram always sends compressed photos with this mime type.
            mimeType = DEFAULT_COMPRESSED_PHOTO_MIME_TYPE;
        } else if (optionalVideo.isPresent()) {
            fileId = optionalVideo.get().getFileId();
            mimeType = optionalVideo.get().getMimeType();
        } else {
            throw new UnexpectedException("Attachment not found");
        }

        byte[] fileData = telegramApiService.getFile(fileId);

        try {
            var detectedMime = tika.detect(fileData);
            if (detectedMime != null && !detectedMime.isBlank()) {
                mimeType = detectedMime;
            }
        } catch (Exception e) {
            log.warn("Unable to detect MIME type via Tika, keeping provided type: {}", mimeType, e);
        }

        Attachment attachment = new Attachment();
        attachment.setData(fileData);
        attachment.setMimeType(mimeType);
        return attachment;
    }

    private Optional<Document> getDocument(Update update) {
        var message = TelegramUtil.getMessage(update);
        if (message == null) {
            return Optional.empty();
        }
        if (message.hasDocument()) {
            return Optional.of(message.getDocument());
        }
        return Optional.empty();
    }

    private Optional<PhotoSize> getPhoto(Update update) {
        var message = TelegramUtil.getMessage(update);
        if (message == null) {
            return Optional.empty();
        }
        if (message.hasPhoto()) {
            return Optional.of(findBestQualityPhoto(message.getPhoto()));
        }
        return Optional.empty();
    }

    private Optional<Video> getVideo(Update update) {
        var message = TelegramUtil.getMessage(update);
        if (message == null) {
            return Optional.empty();
        }
        if (message.hasVideo()) {
            return Optional.of(message.getVideo());
        }
        return Optional.empty();
    }

    private PhotoSize findBestQualityPhoto(List<PhotoSize> photoSizes) {
        Comparator<PhotoSize> photoSizeComparator = Comparator.comparingInt(PhotoSize::getFileSize);
        photoSizes.sort(photoSizeComparator);
        return photoSizes.getLast();
    }
}
