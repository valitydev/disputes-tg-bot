package dev.vality.disputes.tg.bot.provider.service;

import dev.vality.damsel.domain.ProviderRef;
import dev.vality.disputes.provider.*;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.core.event.DisputeWithoutProviderChat;
import dev.vality.disputes.tg.bot.core.service.FileStorage;
import dev.vality.disputes.tg.bot.core.service.HellgateService;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.FileUtil;
import dev.vality.disputes.tg.bot.core.util.InvoiceUtil;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.provider.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.exception.DisputeCreationException;
import dev.vality.disputes.tg.bot.provider.exception.NotSupportedOperationException;
import dev.vality.disputes.tg.bot.provider.util.FormatUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.buildTextWithAttachmentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesHandler implements ProviderDisputesServiceSrv.Iface {

    private final Polyglot polyglot;
    private final FileStorage fileStorage;
    private final TelegramClient telegramClient;
    private final ProviderChatDao chatDao;
    private final ProviderDisputeDao providerDisputeDao;
    private final HellgateService hellgateService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public DisputeCreatedResult createDispute(DisputeParams disputeParams) {
        try {
            //TODO: Simplify
            log.info("Received createDispute request: {}", disputeParams);
            var trxContext = disputeParams.getTransactionContext();
            var invoice = hellgateService.getInvoice(trxContext.getInvoiceId());
            var providerRef = InvoiceUtil.getProviderRef(invoice, trxContext.getPaymentId());
            var providerChat = chatDao.getByProviderId(providerRef.getId());
            if (providerChat.isEmpty()) {
                return createDisputeWithoutProviderChat(disputeParams, providerRef);
            }
            var chat = providerChat.get();
            log.debug("Related chat found successfully: {}", chat);
            return createDisputeWithProviderChat(disputeParams, chat);
        } catch (Exception e) {
            throw new DisputeCreationException("Unable to create dispute, check stacktrace and previous logs", e);
        }
    }

    private @NotNull DisputeCreatedResult createDisputeWithoutProviderChat(DisputeParams disputeParams,
                                                                           ProviderRef providerRef) {
        var providerDispute = buildProviderDispute(disputeParams);
        log.info("Provider chat not found: {}", disputeParams);
        UUID disputeId = providerDisputeDao.save(providerDispute);
        InputFile file = prepareDisputeAttachment(disputeId, disputeParams);
        var event = DisputeWithoutProviderChat.builder()
                .disputeParams(disputeParams)
                .providerRef(providerRef)
                .file(file)
                .build();
        events.publishEvent(event);
        DisputeCreatedResult disputeCreatedResult = new DisputeCreatedResult();
        disputeCreatedResult.setSuccessResult(new DisputeCreatedSuccessResult()
                .setProviderDisputeId(disputeParams.getDisputeId().get()));
        return disputeCreatedResult;
    }

    private @NotNull DisputeCreatedResult createDisputeWithProviderChat(DisputeParams disputeParams, ProviderChat chat)
            throws TelegramApiException {
        ProviderDispute providerDispute = buildProviderDispute(disputeParams, chat);
        UUID disputeId = providerDisputeDao.save(providerDispute);
        log.debug("Created dispute id: {}", disputeId);
        InputFile file = prepareDisputeAttachment(disputeId, disputeParams);

        String text;
        if (chat.getTemplate() != null) {
            text = FormatUtil.prepareTemplate(chat.getTemplate(), disputeParams);
        } else {
            text = polyglot.getText("dispute.provider.create",
                    disputeParams.getTransactionContext().getProviderTrxId(),
                    FormatUtil.formatPaymentId(disputeParams.getTransactionContext()),
                    disputeId.toString());
        }
        var request = buildTextWithAttachmentResponse(chat.getSendToChatId(), text, file);
        request.setMessageThreadId(chat.getSendToTopicId());
        log.info("Sending request to provider");
        Message message = telegramClient.execute(request);
        try {
            providerDisputeDao.updateTgMessageId(Long.valueOf(message.getMessageId()), disputeId);
        } catch (Exception e) {
            // We will be unable to handle this dispute in case of exception. That is why we remove it from
            // telegram and rollback all transactions.
            log.error("Unable to update telegram messageId, source message will be deleted", e);
            DeleteMessage deleteMessage = new DeleteMessage(request.getChatId(), message.getMessageId());
            telegramClient.execute(deleteMessage);
            log.info("Source message was deleted", e);
            throw e;
        }
        DisputeCreatedResult disputeCreatedResult = new DisputeCreatedResult();
        disputeCreatedResult.setSuccessResult(new DisputeCreatedSuccessResult()
                .setProviderDisputeId(disputeId.toString()));
        return disputeCreatedResult;
    }

    private ProviderDispute buildProviderDispute(DisputeParams disputeParams) {
        ProviderDispute providerDispute = new ProviderDispute();
        if (disputeParams.getDisputeId().isPresent()) {
            providerDispute.setId(UUID.fromString(disputeParams.getDisputeId().get()));
        }
        providerDispute.setCreatedAt(LocalDateTime.now());
        providerDispute.setInvoiceId(disputeParams.getTransactionContext().getInvoiceId());
        providerDispute.setPaymentId(disputeParams.getTransactionContext().getPaymentId());
        providerDispute.setProviderTrxId(disputeParams.getTransactionContext().getProviderTrxId());
        return providerDispute;
    }

    private ProviderDispute buildProviderDispute(DisputeParams disputeParams, ProviderChat chat) {
        ProviderDispute providerDispute = buildProviderDispute(disputeParams);
        providerDispute.setChatId(chat.getId());
        return providerDispute;
    }

    @SneakyThrows
    private InputFile prepareDisputeAttachment(UUID disputeId, DisputeParams disputeParams) {
        var document = fileStorage.processFile(disputeParams);
        String filename = FileUtil.createFileName(disputeId, document.getMediaType().getSubtype());
        return TelegramUtil.convertToInputFile(document, filename);
    }

    @Override
    public DisputeStatusResult checkDisputeStatus(DisputeContext disputeContext) {
        throw new NotSupportedOperationException("Dispute must be processed manually");
    }
}