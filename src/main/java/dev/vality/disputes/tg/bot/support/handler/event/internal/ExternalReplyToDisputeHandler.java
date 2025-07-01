package dev.vality.disputes.tg.bot.support.handler.event.internal;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.dao.DaoException;
import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.SupportDisputeReview;
import dev.vality.disputes.tg.bot.core.dto.ProviderMessageDto;
import dev.vality.disputes.tg.bot.core.event.ExternalReplyToDispute;
import dev.vality.disputes.tg.bot.core.handler.InternalEventHandler;
import dev.vality.disputes.tg.bot.core.service.external.DominantCacheServiceImpl;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.FormatUtil;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.dao.SupportDisputeReviewDao;
import dev.vality.disputes.tg.bot.support.service.ResponseParser;
import dev.vality.disputes.tg.bot.support.config.model.ResponsePattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalReplyToDisputeHandler implements InternalEventHandler<ExternalReplyToDispute> {

    private final Polyglot polyglot;
    private final SupportDisputeReviewDao supportDisputeReviewDao;
    private final SupportChatProperties supportChatProperties;
    private final DominantCacheServiceImpl dominantCacheService;
    private final ResponseParser responseParser;
    private final TelegramClient telegramClient;
    private final AdminManagementServiceSrv.Iface adminManagementClient;

    @SneakyThrows
    @Override
    @EventListener
    public void handle(ExternalReplyToDispute event) {
        var replyText = TelegramUtil.extractText(event.getMessage().getUpdate());
        var message = event.getMessage();
        var providerDispute = event.getProviderDispute();
        Optional<ResponsePattern> optionalPattern = responseParser.findMatchingPattern(replyText);
        var pattern = optionalPattern.orElse(new ResponsePattern());
        
        switch (pattern.getResponseType()) {
            case APPROVED -> sendSupportMessage(message, providerDispute, 
                    supportChatProperties.getTopics().getApprovedDisputesProcessing());
            case PENDING -> log.info("Provider reply '{}' matches '{}' type, support won't be notified", 
                    replyText, pattern.getResponseType());
            case DECLINED -> handleDeclinedDispute(providerDispute, pattern, replyText);
            case null -> handleUnknownResponse(message, providerDispute);
        }

        if (pattern.getResponseType() != null) {
            var supportMessage = getReviewPatternSupportMessage(message, providerDispute, pattern);
            supportMessage.setMessageThreadId(supportChatProperties.getTopics().getReviewDisputesPatterns());
            telegramClient.execute(supportMessage);
        }
    }

    private Message sendSupportMessage(ProviderMessageDto message, ProviderDispute providerDispute, 
                                     Integer topicId) throws Exception {
        var supportMessage = getSupportMessage(message, providerDispute);
        Message deliveredMessage;
        
        if (supportMessage instanceof SendMessage sendMessage) {
            sendMessage.setMessageThreadId(topicId);
            deliveredMessage = telegramClient.execute(sendMessage);
        } else if (supportMessage instanceof SendDocument sendDocument) {
            sendDocument.setMessageThreadId(topicId);
            deliveredMessage = telegramClient.execute(sendDocument);
        } else {
            throw new IllegalStateException("Unexpected message type: " + supportMessage.getClass());
        }
        
        return deliveredMessage;
    }

    private void handleDeclinedDispute(ProviderDispute providerDispute, ResponsePattern pattern, 
                                     String replyText) throws Exception {
        CancelParams cancelParams = new CancelParams();
        cancelParams.setInvoiceId(providerDispute.getInvoiceId());
        cancelParams.setPaymentId(providerDispute.getPaymentId());
        cancelParams.setMapping(pattern.getResponseText());
        cancelParams.setCancelReason(replyText);

        CancelParamsRequest cancelParamsRequest = new CancelParamsRequest();
        cancelParamsRequest.setCancelParams(List.of(cancelParams));
        adminManagementClient.cancelPending(cancelParamsRequest);
    }

    private void handleUnknownResponse(ProviderMessageDto message, ProviderDispute providerDispute) 
            throws Exception {
        Message deliveredMessage = sendSupportMessage(message, providerDispute, 
                supportChatProperties.getTopics().getReviewDisputesProcessing());
        
        try {
            supportDisputeReviewDao.save(buildSupportDispute(deliveredMessage, providerDispute));
        } catch (DaoException e) {
            log.error("Unable to save support dispute", e);
            throw e;
        }
    }

    private Object getSupportMessage(ProviderMessageDto message, ProviderDispute providerDispute) {
        String paymentId = FormatUtil.formatPaymentId(providerDispute.getInvoiceId(),
                providerDispute.getPaymentId());
        var provider = dominantCacheService.getProvider(new ProviderRef(message.getProviderChat().getProviderId()));
        String reply = polyglot.getText("dispute.support.response",
                "(%d) %s".formatted(message.getProviderChat().getProviderId(), provider.getName()),
                paymentId,
                providerDispute.getProviderTrxId(),
                extractText(message.getUpdate()));
        
        // Проверяем наличие вложения в сообщении
        var telegramMessage = TelegramUtil.getMessage(message.getUpdate());
        if (telegramMessage != null && TelegramUtil.hasAttachment(telegramMessage)) {
            // Если есть вложение, создаем SendDocument
            InputFile attachment = extractAttachment(telegramMessage);
            if (attachment != null) {
                return TelegramUtil.buildTextWithAttachmentResponse(supportChatProperties.getId(), reply, attachment);
            }
        }
        
        // Если нет вложения, возвращаем обычное текстовое сообщение
        return TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), reply);
    }

    private InputFile extractAttachment(org.telegram.telegrambots.meta.api.objects.message.Message message) {
        if (message.hasPhoto() && message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            // Для фото берем последнее (самое большое) фото
            var photo = message.getPhoto().get(message.getPhoto().size() - 1);
            InputFile inputFile = new InputFile();
            inputFile.setMedia(photo.getFileId());
            return inputFile;
        }
        if (message.hasDocument() && message.getDocument() != null) {
            InputFile inputFile = new InputFile();
            inputFile.setMedia(message.getDocument().getFileId());
            return inputFile;
        }
        if (message.hasVideo() && message.getVideo() != null) {
            InputFile inputFile = new InputFile();
            inputFile.setMedia(message.getVideo().getFileId());
            return inputFile;
        }
        return null;
    }

    private SendMessage getReviewPatternSupportMessage(ProviderMessageDto message, ProviderDispute providerDispute,
                                                       ResponsePattern responsePattern) {
        String paymentId = FormatUtil.formatPaymentId(providerDispute.getInvoiceId(),
                providerDispute.getPaymentId());
        var provider = dominantCacheService.getProvider(new ProviderRef(message.getProviderChat().getProviderId()));
        String reply = polyglot.getText("dispute.support.response-pattern-review",
                "(%d) %s".formatted(message.getProviderChat().getProviderId(), provider.getName()),
                paymentId,
                providerDispute.getProviderTrxId(),
                responsePattern.getResponseType().name(),
                extractText(message.getUpdate()));
        return TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), reply);
    }

    private SupportDisputeReview buildSupportDispute(Message deliveredMessage,
                                                     ProviderDispute providerDispute) {
        SupportDisputeReview supportDispute = new SupportDisputeReview();
        supportDispute.setCreatedAt(LocalDateTime.now());
        supportDispute.setProviderDisputeId(providerDispute.getId());
        supportDispute.setTgMessageId(Long.valueOf(deliveredMessage.getMessageId()));
        supportDispute.setInvoiceId(providerDispute.getInvoiceId());
        supportDispute.setPaymentId(providerDispute.getPaymentId());
        return supportDispute;
    }
}
