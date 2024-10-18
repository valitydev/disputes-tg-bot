package dev.vality.disputes.tg.bot.provider.service;

import dev.vality.disputes.provider.*;
import dev.vality.disputes.tg.bot.common.dao.ChatDao;
import dev.vality.disputes.tg.bot.common.exception.ConfigurationException;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.exception.DisputeCreationException;
import dev.vality.disputes.tg.bot.provider.exception.NotSupportedOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.buildTextWithAttachmentResponse;
import static dev.vality.disputes.tg.bot.provider.util.OptionsUtil.getChatId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesHandler implements ProviderDisputesServiceSrv.Iface {

    private final Polyglot polyglot;
    private final FileStorage fileStorage;
    private final DisputesBot disputesBot;
    private final ChatDao chatDao;
    private final ProviderDisputeDao providerDisputeDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public DisputeCreatedResult createDispute(DisputeParams disputeParams) {
        try {
            log.info("Received createDispute request: {}", disputeParams);
            Chat chat = getChat(disputeParams);
            log.debug("Related chat found successfully: {}", chat);
            ProviderDispute providerDispute = buildProviderDispute(disputeParams, chat);
            UUID disputeId = providerDisputeDao.save(providerDispute);
            log.debug("Created dispute id: {}", disputeId);
            InputFile file = prepareDisputeAttachment(disputeId, disputeParams);

            String text = polyglot.getText(chat.getLocale(), "dispute.provider.create",
                    disputeParams.getTransactionContext().getProviderTrxId(),
                    disputeParams.getTransactionContext().getInvoiceId());
            var request = buildTextWithAttachmentResponse(chat.getId(), text, file);
            log.info("Sending request to provider");
            Message message = disputesBot.execute(request);
            try {
                providerDisputeDao.updateTgMessageId(Long.valueOf(message.getMessageId()), disputeId);
            } catch (Exception e) {
                // We will be unable to handle this dispute in case of exception. That is why we remove it from
                // telegram and rollback all transactions.
                log.error("Unable to update telegram messageId, source message will be deleted", e);
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(request.getChatId());
                deleteMessage.setMessageId(message.getMessageId());
                disputesBot.execute(deleteMessage);
                log.info("Source message was deleted", e);
                throw e;
            }
            DisputeCreatedResult disputeCreatedResult = new DisputeCreatedResult();
            disputeCreatedResult.setSuccessResult(new DisputeCreatedSuccessResult());
            return disputeCreatedResult;
        } catch (Exception e) {
            throw new DisputeCreationException("Unable to create dispute, check stacktrace and previous logs", e);
        }
    }

    private Chat getChat(DisputeParams disputeParams) {
        var chatId = getChatId(disputeParams.getTransactionContext().getTerminalOptions());
        var optionalChat = chatDao.getProviderChatById(chatId);
        if (optionalChat.isEmpty()) {
            log.error("Unable to find telegram chat with id {} in database", chatId);
            throw new ConfigurationException("Unable to find telegram chat with id: " + chatId + " in database");
        }
        return optionalChat.get();
    }

    private ProviderDispute buildProviderDispute(DisputeParams disputeParams, Chat chat) {
        ProviderDispute providerDispute = new ProviderDispute();
        providerDispute.setCreatedAt(LocalDateTime.now());
        providerDispute.setChatId(chat.getId());
        providerDispute.setInvoiceId(disputeParams.getTransactionContext().getInvoiceId());
        providerDispute.setPaymentId(disputeParams.getTransactionContext().getPaymentId());
        providerDispute.setProviderTrxId(disputeParams.getTransactionContext().getProviderTrxId());
        if (disputeParams.getReason().isPresent()) {
            providerDispute.setReason(disputeParams.getReason().get());
        }
        return providerDispute;
    }

    private InputFile prepareDisputeAttachment(UUID disputeId, DisputeParams disputeParams) {
        var document = fileStorage.processAttachment(disputeParams);
        InputFile file = new InputFile();
        file.setMedia(new ByteArrayInputStream(document.getContent()),
                createFileName(disputeId, document.getMediaType().getSubtype()));
        return file;
    }

    private String createFileName(UUID disputeId, String extension) {
        return disputeId.toString() + "." + extension;
    }

    @Override
    public DisputeStatusResult checkDisputeStatus(DisputeContext disputeContext) throws TException {
        throw new NotSupportedOperationException("Dispute must be processed manually");
    }
}