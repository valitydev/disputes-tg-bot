package dev.vality.disputes.tg.bot.provider.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.service.FileStorage;
import dev.vality.disputes.tg.bot.core.util.FileUtil;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeAttachmentService {

    private final FileStorage fileStorage;

    @SneakyThrows
    public InputFile prepareDisputeAttachment(UUID disputeId, DisputeParams disputeParams) {
        var document = fileStorage.processFile(disputeParams);
        String filename = FileUtil.createFileName(disputeId, document.getMediaType().getSubtype());
        return TelegramUtil.convertToInputFile(document, filename);
    }
} 