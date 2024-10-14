package dev.vality.disputes.tg.bot.provider.handler.event;

import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderReply;
import dev.vality.disputes.tg.bot.core.event.ExternalReplyToDispute;
import dev.vality.disputes.tg.bot.core.exception.NotFoundException;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.dao.ProviderReplyDao;
import dev.vality.disputes.tg.bot.core.dto.ProviderMessageDto;
import dev.vality.disputes.tg.bot.provider.handler.ProviderMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRepliedToDisputeHandler implements ProviderMessageHandler {

    private final ApplicationEventPublisher events;
    private final ProviderDisputeDao providerDisputeDao;
    private final ProviderReplyDao providerReplyDao;

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
        try {
            events.publishEvent(ExternalReplyToDispute.builder()
                    .message(message)
                    .providerDispute(providerDispute).build());
        } catch (Exception e) {
            log.error("oops", e);
            throw new RuntimeException(e);
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
}
