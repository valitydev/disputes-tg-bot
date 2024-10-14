package dev.vality.disputes.tg.bot.merchant.converter;

import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.swag.disputes.model.CreateRequestAttachmentsInner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateToCreateRequestAttachmentsInnerConverter {

    public CreateRequestAttachmentsInner convert(Update update, DisputesBot disputesBot) throws TelegramApiException,
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
            mimeType = "image/jpeg";
        }


        var getFileRequest = GetFile.builder()
                .fileId(fileId)
                .build();
        org.telegram.telegrambots.meta.api.objects.File file = disputesBot.execute(getFileRequest);
        String filePath = file.getFilePath();
        java.io.File attachedFile = disputesBot.downloadFile(filePath);
        byte[] fileContent = Files.readAllBytes(attachedFile.toPath());

        CreateRequestAttachmentsInner createRequestAttachmentsInner = new CreateRequestAttachmentsInner();
        createRequestAttachmentsInner.setData(fileContent);
        createRequestAttachmentsInner.setMimeType(mimeType);
        return createRequestAttachmentsInner;
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


}
