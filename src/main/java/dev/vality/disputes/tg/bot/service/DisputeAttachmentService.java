package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.service.external.FileStorageService;
import dev.vality.disputes.tg.bot.util.FileUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeAttachmentService {

    private final FileStorageService fileStorageService;

    @SneakyThrows
    public InputFile prepareDisputeAttachment(UUID disputeId, DisputeParams disputeParams) {
        var document = fileStorageService.processFile(disputeParams);
        String filename = FileUtil.createFileName(disputeId, document.getMediaType().getSubtype());
        return TelegramUtil.convertToInputFile(document, filename);
    }
} 