package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.provider.DisputeParams;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import dev.vality.disputes.tg.bot.util.TemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.UUID;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.buildTextWithAttachmentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final Polyglot polyglot;
    private final TelegramClient telegramClient;
    private final ProviderDisputeDao providerDisputeDao;

    public void sendDisputeNotification(ProviderChat chat, DisputeParams disputeParams,
                                        InputFile file, UUID disputeId) throws TelegramApiException {
        String text = prepareNotificationText(chat, disputeParams, disputeId);
        var request = buildTextWithAttachmentResponse(chat.getSendToChatId(), text, file);
        request.setMessageThreadId(chat.getSendToTopicId());
        
        log.info("Sending dispute notification to provider");
        Message message = telegramClient.execute(request);
        try {
            providerDisputeDao.updateTgMessageId(Long.valueOf(message.getMessageId()), disputeId);
        } catch (Exception e) {
            log.error("Unable to update telegram messageId, source message will be deleted", e);
            deleteMessage(request.getChatId(), message.getMessageId());
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

    private void deleteMessage(String chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
            telegramClient.execute(deleteMessage);
            log.info("Source message was deleted");
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
    }
} 