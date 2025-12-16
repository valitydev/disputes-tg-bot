package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeclineHandler implements AdminMessageHandler {

    private final AdminChatProperties adminChatProperties;
    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramApiService telegramApiService;
    private final ProviderDisputeDao providerDisputeDao;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage() || update.getMessage().getMessageThreadId() == null) {
            return false;
        }

        var threadId = update.getMessage().getMessageThreadId();
        return adminChatProperties.getTopics().getBatchDisputesProcessing().equals(threadId);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void handle(Update update) {
        log.info("[{}] Processing decline dispute request from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        var disputeInfos = TextParsingUtil.getDeclineDisputeInfos(messageText);
        log.info("[{}] Parsed {} decline dispute requests from {}",
                update.getUpdateId(),
                disputeInfos.size(), TelegramUtil.extractUserInfo(update));

        if (!disputeInfos.isEmpty()) {
            CancelParamsRequest cancelParamsRequest = new CancelParamsRequest();
            cancelParamsRequest.setCancelParams(disputeInfos.stream().map(this::convertToCancelParams).toList());
            adminManagementClient.cancelPending(cancelParamsRequest);
            disputeInfos.forEach(event -> {
                        log.info("Updating provider dispute info: {}", event);
                        providerDisputeDao.updateReason(event.getInvoiceId(), event.getPaymentId(),
                                event.getResponseText());
                        log.info("Successfully updated provider dispute info: {}", event);
                    }
            );
            log.info("[{}] Successfully processed decline disputes request", update.getUpdateId());
            telegramApiService.setThumbUpReaction(adminChatProperties.getId(), update.getMessage().getMessageId());
        }
    }

    private CancelParams convertToCancelParams(DisputeInfoDto disputeInfoDto) {
        CancelParams cancelParams = new CancelParams();
        cancelParams.setInvoiceId(disputeInfoDto.getInvoiceId());
        cancelParams.setPaymentId(disputeInfoDto.getPaymentId());
        cancelParams.setMapping(disputeInfoDto.getResponseText());
        return cancelParams;
    }

}
