package dev.vality.disputes.tg.bot.handler.provider;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.dao.DaoException;
import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.config.model.ResponsePattern;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.domain.tables.pojos.AdminDisputeReview;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderReply;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.dao.ProviderReplyDao;
import dev.vality.disputes.tg.bot.dto.ProviderMessageDto;
import dev.vality.disputes.tg.bot.exception.NotFoundException;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.ResponseParser;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRepliedToDisputeHandler implements ProviderMessageHandler {

    private final ProviderDisputeDao providerDisputeDao;
    private final ProviderReplyDao providerReplyDao;
    private final Polyglot polyglot;
    private final AdminDisputeReviewDao adminDisputeReviewDao;
    private final AdminChatProperties adminChatProperties;
    private final DominantService dominantCacheService;
    private final ResponseParser responseParser;
    private final TelegramApiService telegramApiService;
    private final AdminManagementServiceSrv.Iface adminManagementClient;

    @Override
    public boolean filter(ProviderMessageDto message) {
        // Message contains text
        String text = extractText(message.getUpdate());
        if (text == null) {
            return false;
        }
        var tgMessage = message.getUpdate().getMessage();
        // Is reply
        if (!tgMessage.isReply()) {
            return false;
        }

        // Is most likely reply to a dispute message, because it has attachment and payment/dispute ids
        boolean originMessageHasAttachment = TelegramUtil.hasAttachment(tgMessage.getReplyToMessage());
        if (!originMessageHasAttachment) {
            log.debug("Origin message has no attachment, handler won't be applied");
            return false;
        }

        var originDisputeInfo =
                TextParsingUtil.getDisputeInfo(tgMessage.getReplyToMessage().getCaption());
        if (originDisputeInfo.isEmpty()) {
            log.debug("Origin message has no dispute info, handler won't be applied");
            return false;
        }
        var disputes = providerDisputeDao.get(originDisputeInfo.get().getInvoiceId(),
                originDisputeInfo.get().getPaymentId());
        log.debug("Found {} applicable disputes for dispute info: {}", disputes.size(), originDisputeInfo.get());
        return !disputes.isEmpty();
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(ProviderMessageDto message) {
        String replyText = TelegramUtil.extractText(message.getUpdate());
        var providerDispute = getProviderDispute(message);
        ProviderReply providerReply = new ProviderReply();
        providerReply.setDisputeId(providerDispute.getId());
        providerReply.setReplyText(replyText);
        providerReply.setRepliedAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(message.getUpdate().getMessage().getDate()), ZoneOffset.UTC));
        providerReply.setUsername(TelegramUtil.extractUserInfo(message.getUpdate()));
        providerReplyDao.save(providerReply);


        Optional<ResponsePattern> optionalPattern = responseParser.findMatchingPattern(replyText);
        var pattern = optionalPattern.orElse(new ResponsePattern());

        switch (pattern.getResponseType()) {
            case APPROVED -> sendAdminMessage(message, providerDispute,
                    adminChatProperties.getTopics().getApprovedDisputesProcessing());
            case PENDING -> log.info("Provider reply '{}' matches '{}' type, support won't be notified",
                    replyText, pattern.getResponseType());
            case DECLINED -> handleDeclinedDispute(providerDispute, pattern, replyText);
            case null -> handleUnknownResponse(message, providerDispute);
        }

        if (pattern.getResponseType() != null) {
            var supportMessage = getReviewPatternSupportMessage(message, providerDispute, pattern);
            telegramApiService.sendMessage(supportMessage, adminChatProperties.getId(),
                    adminChatProperties.getTopics().getReviewDisputesPatterns());
        }
    }

    private ProviderDispute getProviderDispute(ProviderMessageDto message) {
        Long replyToMessageId = Long.valueOf(message.getUpdate().getMessage().getReplyToMessage().getMessageId());
        ProviderDispute providerDispute = providerDisputeDao.get(replyToMessageId,
                message.getProviderChat().getId());
        if (providerDispute != null) {
            return providerDispute;
        }

        var disputeInfoOptional =
                TextParsingUtil.getDisputeInfo(message.getUpdate().getMessage().getReplyToMessage().getCaption());
        if (disputeInfoOptional.isEmpty()) {
            throw new NotFoundException("Unable to find dispute info");
        }
        var disputeInfo = disputeInfoOptional.get();
        var disputes = providerDisputeDao.get(disputeInfo.getInvoiceId(), disputeInfo.getPaymentId());
        if (disputes == null || disputes.isEmpty()) {
            throw new NotFoundException("Unable to find dispute info");
        }
        log.warn("Found {} provider disputes, but will return only first one", disputes.size());
        return disputes.getFirst();
    }

    private Optional<Message> sendAdminMessage(ProviderMessageDto message, ProviderDispute providerDispute,
                                               Integer adminTopicId) {
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
        if (TelegramUtil.hasAttachment(telegramMessage)) {
            // Если есть вложение, создаем SendDocument
            InputFile attachment = extractAttachment(telegramMessage);
            if (attachment != null) {
                return telegramApiService.sendMessageWithAttachment(reply, adminChatProperties.getId(), adminTopicId,
                        attachment);
            }
        }

        // Если нет вложения, возвращаем обычное текстовое сообщение
        return telegramApiService.sendMessage(reply, adminChatProperties.getId(), adminTopicId);
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

    private void handleUnknownResponse(ProviderMessageDto message, ProviderDispute providerDispute) {
        var adminMessage = sendAdminMessage(message, providerDispute,
                adminChatProperties.getTopics().getReviewDisputesProcessing());
        if (adminMessage.isEmpty()) {
            log.error("Unable to send info to admin chat. Check previous logs.");
            return;
        }

        try {
            adminDisputeReviewDao.save(buildSupportDispute(adminMessage.get(), providerDispute));
        } catch (DaoException e) {
            log.error("Unable to save support dispute", e);
            throw e;
        }
    }

    private String getReviewPatternSupportMessage(ProviderMessageDto message, ProviderDispute providerDispute,
                                                  ResponsePattern responsePattern) {
        String paymentId = FormatUtil.formatPaymentId(providerDispute.getInvoiceId(),
                providerDispute.getPaymentId());
        var provider = dominantCacheService.getProvider(new ProviderRef(message.getProviderChat().getProviderId()));
        return polyglot.getText("dispute.support.response-pattern-review",
                "(%d) %s".formatted(message.getProviderChat().getProviderId(), provider.getName()),
                paymentId,
                providerDispute.getProviderTrxId(),
                responsePattern.getResponseType().name(),
                extractText(message.getUpdate()));
    }

    private InputFile extractAttachment(org.telegram.telegrambots.meta.api.objects.message.Message message) {
        if (message.hasPhoto() && message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            // Для фото берем последнее (самое большое) фото
            var photo = message.getPhoto().getLast();
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

    private AdminDisputeReview buildSupportDispute(Message deliveredMessage,
                                                   ProviderDispute providerDispute) {
        AdminDisputeReview supportDispute = new AdminDisputeReview();
        supportDispute.setCreatedAt(LocalDateTime.now());
        supportDispute.setProviderDisputeId(providerDispute.getId());
        supportDispute.setTgMessageId(Long.valueOf(deliveredMessage.getMessageId()));
        supportDispute.setInvoiceId(providerDispute.getInvoiceId());
        supportDispute.setPaymentId(providerDispute.getPaymentId());
        return supportDispute;
    }
}
