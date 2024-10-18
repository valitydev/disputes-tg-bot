package dev.vality.disputes.tg.bot.provider.handler;

import dev.vality.disputes.admin.*;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.exception.ConfigurationException;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.provider.dao.SupportDisputeDao;
import dev.vality.disputes.tg.bot.provider.service.ResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;

import java.time.LocalDateTime;
import java.util.List;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportRepliedToDisputeHandler implements CommonHandler {

    private final Polyglot polyglot;
    private final SupportDisputeDao supportDisputeDao;
    private final ResponseParser providerResponseParser;
    private final ManualParsingServiceSrv.Iface manualParsingClient;

    @Override
    public boolean filter(MessageDto messageDto) {
        // Message contains text
        String messageText = extractText(messageDto.getUpdate());
        if (messageText == null) {
            return false;
        }
        // Chat belongs to support
        if (!ChatType.support.equals(messageDto.getChat().getType())) {
            return false;
        }
        // Is reply
        if (!messageDto.getUpdate().getMessage().isReply()) {
            return false;
        }
        // Is reply to dispute message
        Long replyToMessageId = Long.valueOf(messageDto.getUpdate().getMessage().getReplyToMessage().getMessageId());
        return supportDisputeDao.isReplyToDisputeMessage(replyToMessageId, messageDto.getChat().getId());
    }

    @Override
    @SneakyThrows
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {

        var update = messageDto.getUpdate();
        Long replyToMessageId =
                Long.valueOf(update.getMessage().getReplyToMessage().getMessageId());
        var optionalDispute = supportDisputeDao.getReplyToMessageId(replyToMessageId, messageDto.getChat().getId());
        var dispute = optionalDispute.orElseThrow(
                () -> new ConfigurationException("Unreachable point, check application configuration"));

        if (providerResponseParser.isApprovedBySupportResponse(extractText(update))) {
            ApproveParams approveParams = new ApproveParams();
            approveParams.setSkipCallHgForCreateAdjustment(true);
            approveParams.setDisputeId(dispute.getProviderDisputeId().toString());
            manualParsingClient.approvePending(new ApproveParamsRequest().setApproveParams(List.of(approveParams)));

            dispute.setIsApproved(true);
            dispute.setRepliedAt(LocalDateTime.now());
            supportDisputeDao.update(dispute);

            var reaction = ReactionTypeEmoji.builder().emoji("👍").build();
            var messageReaction = new SetMessageReaction();
            messageReaction.setChatId(messageDto.getChat().getId());
            messageReaction.setMessageId(messageDto.getUpdate().getMessage().getMessageId());
            messageReaction.setReactionTypes(List.of(reaction));
            disputesBot.execute(messageReaction);
        } else {
            CancelParams cancelParams = new CancelParams();
            cancelParams.setDisputeId(dispute.getProviderDisputeId().toString());

            String text = extractText(update);
            cancelParams.setCancelReason(text);

            manualParsingClient.cancelPending(new CancelParamsRequest(List.of(cancelParams)));

            dispute.setIsApproved(false);
            dispute.setRepliedAt(LocalDateTime.now());
            dispute.setReplyText(text);
            supportDisputeDao.save(dispute);

            var messageReaction = getSetMessageReaction(getChatId(messageDto.getUpdate()),
                    messageDto.getUpdate().getMessage().getMessageId(), "👎");
            disputesBot.execute(messageReaction);
        }
    }
}
