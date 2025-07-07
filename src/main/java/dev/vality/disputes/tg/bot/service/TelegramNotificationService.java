package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import dev.vality.disputes.tg.bot.util.TemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final Polyglot polyglot;
    private final TelegramApiService telegramApiService;
    private final ProviderDisputeDao providerDisputeDao;

    @Transactional
    public void sendDisputeNotification(ProviderChat chat, DisputeParams disputeParams,
                                        InputFile file, UUID disputeId) {
        String text = prepareNotificationText(chat, disputeParams, disputeId);
        log.info("Sending dispute notification to provider");
        var response = telegramApiService.sendMessageWithDocument(text, chat.getSendToChatId(), file);
        if (response.isEmpty()) {
            log.warn("Unable to send dispute notification to provider");
            return;
        }
        try {
            providerDisputeDao.updateTgMessageId(Long.valueOf(response.get().getMessageId()), disputeId);
        } catch (Exception e) {
            log.error("Unable to update telegram messageId, source message will be deleted", e);
            telegramApiService.deleteMessage(String.valueOf(response.get().getChatId()), response.get().getMessageId());
            throw e;
        }
    }

    private String prepareNotificationText(ProviderChat chat, DisputeParams disputeParams, UUID disputeId) {
        if (chat.getTemplate() != null) {
            return TemplateUtil.prepareTemplate(chat.getTemplate(), disputeParams);
        } else {
            return polyglot.getText("dispute.provider.create",
                    disputeParams.getTransactionContext().getProviderTrxId(),
                    FormatUtil.formatPaymentId(disputeParams.getTransactionContext()),
                    disputeId.toString());
        }
    }
} 