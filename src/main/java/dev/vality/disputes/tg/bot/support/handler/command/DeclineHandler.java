package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParams;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.core.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeclineHandler implements SupportMessageHandler {

    private final SupportChatProperties supportChatProperties;
    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramClient telegramClient;
    private final ApplicationEventPublisher events;

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
        return supportChatProperties.getTopics().getBatchDisputesProcessing().equals(threadId);
    }

    @Override
    @SneakyThrows
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
            disputeInfos.forEach(events::publishEvent);
            log.info("[{}] Successfully processed decline disputes request", update.getUpdateId());

            try {
                var reaction = TelegramUtil.getSetMessageReaction(supportChatProperties.getId(),
                        update.getMessage().getMessageId(), "👍");
                telegramClient.execute(reaction);
            } catch (Exception e) {
                log.warn("[{}] Unable to set 'thumb up' reaction", update.getUpdateId(), e);
            }
        }
    }

    private CancelParams convertToCancelParams(DisputeInfoDto disputeInfoDto) {
        CancelParams cancelParams = new CancelParams();
        cancelParams.setInvoiceId(disputeInfoDto.getInvoiceId());
        cancelParams.setPaymentId(disputeInfoDto.getPaymentId());
        cancelParams.setCancelReason(disputeInfoDto.getResponseText());
        cancelParams.setMapping(disputeInfoDto.getResponseText());
        return cancelParams;
    }

}
