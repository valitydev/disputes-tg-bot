package dev.vality.disputes.tg.bot.support.handler.event;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.core.exception.ConfigurationException;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.dao.SupportDisputeReviewDao;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportRepliedToDisputeHandler implements SupportMessageHandler {

    private final ProviderDisputeDao providerDisputeDao;
    private final SupportDisputeReviewDao supportDisputeReviewDao;
    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final SupportChatProperties supportChatProperties;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(Update update) {
        // Message contains text
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }
        // Is reply
        if (!update.getMessage().isReply()) {
            return false;
        }
        // Is reply to dispute message
        Long replyToMessageId = Long.valueOf(update.getMessage().getReplyToMessage().getMessageId());
        return supportDisputeReviewDao.isReplyToDisputeMessage(replyToMessageId, supportChatProperties.getId());
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {

        Long replyToMessageId =
                Long.valueOf(update.getMessage().getReplyToMessage().getMessageId());
        var optionalDispute = supportDisputeReviewDao.getReplyToMessageId(replyToMessageId,
                supportChatProperties.getId());
        var dispute = optionalDispute.orElseThrow(
                () -> new ConfigurationException("Unreachable point, check application configuration"));

        CancelParams cancelParams = new CancelParams();
        //TODO: Extract dispute info


        String text = extractText(update);
        if (dispute.getInvoiceId() == null) {
            var providerDisputeInfo = providerDisputeDao.get(dispute.getProviderDisputeId());
            cancelParams.setInvoiceId(providerDisputeInfo.getInvoiceId());
            cancelParams.setPaymentId(providerDisputeInfo.getPaymentId());
        } else {
            cancelParams.setInvoiceId(dispute.getInvoiceId());
            cancelParams.setPaymentId(dispute.getPaymentId());
        }
        cancelParams.setCancelReason(text);
        cancelParams.setMapping(text);

        adminManagementClient.cancelPending(new CancelParamsRequest(List.of(cancelParams)));

        dispute.setIsApproved(false);
        dispute.setRepliedAt(LocalDateTime.now());
        dispute.setReplyText(text);
        supportDisputeReviewDao.update(dispute);

        var messageReaction = getSetMessageReaction(getChatId(update),
                update.getMessage().getMessageId(), "👍");
        telegramClient.execute(messageReaction);
    }
}
