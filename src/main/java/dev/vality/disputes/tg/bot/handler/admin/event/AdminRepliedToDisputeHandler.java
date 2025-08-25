package dev.vality.disputes.tg.bot.handler.admin.event;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.dao.ProviderReplyDao;
import dev.vality.disputes.tg.bot.exception.ConfigurationException;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRepliedToDisputeHandler implements AdminMessageHandler {

    private final AdminDisputeReviewDao adminDisputeReviewDao;
    private final ProviderReplyDao providerReplyDao;
    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramApiService telegramApiService;

    @Override
    public boolean filter(Update update) {
        // Message contains text
        String messageText = TelegramUtil.extractText(update);
        if (messageText == null) {
            return false;
        }
        // Is reply
        if (!update.getMessage().isReply()) {
            return false;
        }
        // Is reply to dispute message
        Long replyToMessageId = Long.valueOf(update.getMessage().getReplyToMessage().getMessageId());
        return adminDisputeReviewDao.isReplyToDisputeMessage(replyToMessageId);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {

        Long replyToMessageId =
                Long.valueOf(update.getMessage().getReplyToMessage().getMessageId());
        var optionalDispute = adminDisputeReviewDao.getReplyToMessageId(replyToMessageId);
        var dispute = optionalDispute.orElseThrow(
                () -> new ConfigurationException("Unreachable point, check application configuration"));

        CancelParams cancelParams = new CancelParams();
        String text = TelegramUtil.extractText(update);
        cancelParams.setInvoiceId(dispute.getInvoiceId());
        cancelParams.setPaymentId(dispute.getPaymentId());
        cancelParams.setProviderMessage(providerReplyDao.getById(dispute.getProviderReplyId()).getReplyText());
        cancelParams.setMapping(text);

        adminManagementClient.cancelPending(new CancelParamsRequest(List.of(cancelParams)));

        dispute.setIsApproved(false);
        dispute.setRepliedAt(LocalDateTime.now());
        adminDisputeReviewDao.update(dispute);

        telegramApiService.setThumbUpReaction(TelegramUtil.getChatId(update), update.getMessage().getMessageId());
    }
}
