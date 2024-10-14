package dev.vality.disputes.tg.bot.support.handler.event.internal;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.dao.DaoException;
import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.SupportDisputeReview;
import dev.vality.disputes.tg.bot.core.event.ExternalReplyToDispute;
import dev.vality.disputes.tg.bot.core.handler.InternalEventHandler;
import dev.vality.disputes.tg.bot.core.service.DominantCacheServiceImpl;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.provider.dao.SupportDisputeReviewDao;
import dev.vality.disputes.tg.bot.core.dto.ProviderMessageDto;
import dev.vality.disputes.tg.bot.provider.service.ResponseParser;
import dev.vality.disputes.tg.bot.provider.service.ResponsePattern;
import dev.vality.disputes.tg.bot.provider.util.FormatUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
            case APPROVED -> {
                var supportMessage = getSupportMessage(message, providerDispute);
                supportMessage.setMessageThreadId(
                        supportChatProperties.getTopics().getApprovedDisputesProcessing());
                telegramClient.execute(supportMessage);
            }
            case PENDING ->
                    log.info("Provider reply '{}' matches 'pending' type, support won't be notified", replyText);
            case DECLINED -> {
                CancelParams cancelParams = new CancelParams();
                cancelParams.setInvoiceId(providerDispute.getInvoiceId());
                cancelParams.setPaymentId(providerDispute.getPaymentId());
                cancelParams.setMapping(pattern.getResponseText());
                cancelParams.setCancelReason(replyText);

                CancelParamsRequest cancelParamsRequest = new CancelParamsRequest();
                cancelParamsRequest.setCancelParams(List.of(cancelParams));
                adminManagementClient.cancelPending(cancelParamsRequest);
            }
            case null -> {
                var supportMessage = getSupportMessage(message, providerDispute);
                supportMessage.setMessageThreadId(
                        supportChatProperties.getTopics().getReviewDisputesProcessing());
                Message deliveredMessage = telegramClient.execute(supportMessage);
                try {
                    supportDisputeReviewDao.save(buildSupportDispute(deliveredMessage, providerDispute));
                } catch (DaoException e) {
                    log.error("Unable to save support dispute", e);
                    throw e;
                }
            }
        }

        if (pattern.getResponseType() != null) {
            var supportMessage = getReviewPatternSupportMessage(message, providerDispute, pattern);
            supportMessage.setMessageThreadId(
                    supportChatProperties.getTopics().getReviewDisputesPatterns());
            telegramClient.execute(supportMessage);
        }
    }

    private SendMessage getSupportMessage(ProviderMessageDto message, ProviderDispute providerDispute) {
        String paymentId = FormatUtil.formatPaymentId(providerDispute.getInvoiceId(),
                providerDispute.getPaymentId());
        var provider = dominantCacheService.getProvider(new ProviderRef(message.getProviderChat().getProviderId()));
        String reply = polyglot.getText("dispute.support.response",
                "(%d) %s".formatted(message.getProviderChat().getProviderId(), provider.getName()),
                paymentId,
                providerDispute.getProviderTrxId(),
                extractText(message.getUpdate()));
        return TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), reply);
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
        return supportDispute;
    }
}
