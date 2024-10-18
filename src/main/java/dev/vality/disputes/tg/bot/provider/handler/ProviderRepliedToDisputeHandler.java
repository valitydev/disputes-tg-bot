package dev.vality.disputes.tg.bot.provider.handler;

import dev.vality.dao.DaoException;
import dev.vality.disputes.tg.bot.common.dao.ChatDao;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TelegramUtil;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.domain.tables.pojos.SupportDispute;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.dao.SupportDisputeDao;
import dev.vality.disputes.tg.bot.provider.service.ResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRepliedToDisputeHandler implements CommonHandler {

    private final ChatDao chatDao;
    private final Polyglot polyglot;
    private final SupportDisputeDao supportDisputeDao;
    private final ProviderDisputeDao providerDisputeDao;

    @Override
    public boolean filter(MessageDto messageDto) {
        // Message contains text
        String text = extractText(messageDto.getUpdate());
        if (text == null) {
            return false;
        }
        // Chat belongs to provider
        if (!ChatType.provider.equals(messageDto.getChat().getType())) {
            return false;
        }
        // Is reply
        if (!messageDto.getUpdate().getMessage().isReply()) {
            return false;
        }
        // Is reply to dispute message
        Long replyToMessageId = Long.valueOf(messageDto.getUpdate().getMessage().getReplyToMessage().getMessageId());
        return providerDisputeDao.isReplyToDisputeMessage(replyToMessageId, messageDto.getChat().getId());
    }

    @Override
    @SneakyThrows
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        Chat supportChat = chatDao.getSupportChat();
        Long replyToMessageId = Long.valueOf(messageDto.getUpdate().getMessage().getReplyToMessage().getMessageId());
        ProviderDispute providerDispute = providerDisputeDao.get(replyToMessageId,
                messageDto.getChat().getId());
        Message deliveredMessage;
        try {
            String reply = polyglot.getText(supportChat.getLocale(), "dispute.support.response",
                    providerDispute.getId(), providerDispute.getInvoiceId(), providerDispute.getProviderTrxId(),
                    extractText(messageDto.getUpdate()));
            var message = TelegramUtil.buildPlainTextResponse(supportChat.getId(), reply);
            deliveredMessage = disputesBot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Unable to send message to support chat", e);
            throw e;
        }

        try {
            supportDisputeDao.save(buildSupportDispute(supportChat, deliveredMessage, providerDispute));
        } catch (DaoException e) {
            log.error("Unable to save support dispute", e);
        }
    }

    private SupportDispute buildSupportDispute(Chat supportChat, Message deliveredMessage,
                                               ProviderDispute providerDispute) {
        SupportDispute supportDispute = new SupportDispute();
        supportDispute.setChatId(supportChat.getId());
        supportDispute.setCreatedAt(LocalDateTime.now());
        supportDispute.setProviderDisputeId(providerDispute.getId());
        supportDispute.setTgMessageId(Long.valueOf(deliveredMessage.getMessageId()));
        return supportDispute;
    }
}
