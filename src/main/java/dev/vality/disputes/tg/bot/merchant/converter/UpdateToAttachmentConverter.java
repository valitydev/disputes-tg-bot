package dev.vality.disputes.tg.bot.merchant.converter;

import dev.vality.disputes.merchant.Attachment;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateToAttachmentConverter {

    private static final String DEFAULT_COMPRESSED_PHOTO_MIME_TYPE = "image/jpeg";

    public Attachment convert(Update update, DisputesBot disputesBot) throws TelegramApiException,
            IOException {
        String fileId;
        String mimeType;
        var optionalDocument = getDocument(update);
        if (optionalDocument.isPresent()) {
            fileId = optionalDocument.get().getFileId();
            mimeType = optionalDocument.get().getMimeType();
        } else {
            var optionalPhoto = getPhoto(update);
            // If no fileId present, throw exception, cause this condition must be prevented on earlier stages.
            fileId = optionalPhoto.map(PhotoSize::getFileId).orElseThrow();
            // Telegram always sends compressed photos with this mime type.
            mimeType = DEFAULT_COMPRESSED_PHOTO_MIME_TYPE;
        }

        Attachment attachment = new Attachment();
        attachment.setData(getFileContent(fileId, disputesBot));
        attachment.setMimeType(mimeType);
        return attachment;
    }

    private Optional<Document> getDocument(Update update) {
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            return Optional.of(update.getMessage().getDocument());
        }
        return Optional.empty();
    }

    private Optional<PhotoSize> getPhoto(Update update) {
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            return Optional.of(update.getMessage().getPhoto().getFirst());
        }
        return Optional.empty();
    }

    @SneakyThrows
    private byte[] getFileContent(String fileId, DisputesBot disputesBot) {
        var getFileRequest = GetFile.builder()
                .fileId(fileId)
                .build();
        log.debug("Downloading file with fileId: {}", fileId);
        org.telegram.telegrambots.meta.api.objects.File file = disputesBot.execute(getFileRequest);
        String filePath = file.getFilePath();
        java.io.File attachedFile = disputesBot.downloadFile(filePath);
        return Files.readAllBytes(attachedFile.toPath());
    }


}
