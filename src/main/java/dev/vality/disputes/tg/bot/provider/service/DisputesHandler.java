package dev.vality.disputes.tg.bot.provider.service;

import dev.vality.disputes.provider.*;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.disputes.tg.bot.core.service.DominantCacheServiceImpl;
import dev.vality.disputes.tg.bot.core.service.FileStorage;
import dev.vality.disputes.tg.bot.core.service.HellgateService;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.InvoiceUtil;
import dev.vality.disputes.tg.bot.provider.dao.ProviderChatDao;
import dev.vality.disputes.tg.bot.provider.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.provider.exception.DisputeCreationException;
import dev.vality.disputes.tg.bot.provider.exception.NotSupportedOperationException;
import dev.vality.disputes.tg.bot.provider.util.FormatUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
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
    private final DominantCacheServiceImpl dominantCacheService;
    private final SupportChatProperties supportChatProperties;

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
                var providerDispute = buildProviderDispute(disputeParams);
                log.info("Provider chat not found, sending to default support topic: {}", disputeParams);
                UUID disputeId = providerDisputeDao.save(providerDispute);
                var provider = dominantCacheService.getProvider(providerRef);

                String text = polyglot.getText("dispute.provider.created-wo-chat",
                        providerRef.getId(), provider.getName(),
                        trxContext.getProviderTrxId(),
                        trxContext.getInvoiceId() + "." + trxContext.getPaymentId(),
                        disputeParams.getDisputeId().get());
                InputFile file = prepareDisputeAttachment(disputeId, disputeParams);
                var request = buildTextWithAttachmentResponse(supportChatProperties.getId(), text, file);
                request.setMessageThreadId(supportChatProperties.getTopics().getUnknownProvider());
                telegramClient.execute(request);

                DisputeCreatedResult disputeCreatedResult = new DisputeCreatedResult();
                disputeCreatedResult.setSuccessResult(new DisputeCreatedSuccessResult()
                        .setProviderDisputeId(disputeParams.getDisputeId().get()));
                return disputeCreatedResult;
            }
            var chat = providerChat.get();
            log.debug("Related chat found successfully: {}", chat);
            ProviderDispute providerDispute = buildProviderDispute(disputeParams, chat);
            UUID disputeId = providerDisputeDao.save(providerDispute);
            log.debug("Created dispute id: {}", disputeId);
            InputFile file = prepareDisputeAttachment(disputeId, disputeParams);

            String text;
            if (chat.getTemplate() != null) {
                text = prepareTemplate(chat.getTemplate(), disputeParams);
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
        } catch (Exception e) {
            throw new DisputeCreationException("Unable to create dispute, check stacktrace and previous logs", e);
        }
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
        var document = fileStorage.processAttachment(disputeParams);
        InputFile file = new InputFile();
        file.setMedia(new ByteArrayInputStream(document.getContent()),
                createFileName(disputeId, document.getMediaType().getSubtype()));
        return file;
    }

    private String prepareTemplate(String template, DisputeParams disputeParams) {
        return template.replaceAll("\\$\\{dispute_id}", disputeParams.disputeId)
                .replaceAll("\\$\\{invoice_id}", disputeParams.getTransactionContext().getInvoiceId())
                .replaceAll("\\$\\{payment_id}", disputeParams.getTransactionContext().getPaymentId())
                .replaceAll("\\$\\{provider_trx_id}", disputeParams.getTransactionContext().getProviderTrxId())
                .replaceAll("\\$\\{amount_formatted}", getFormattedAmount(disputeParams));
    }

    private String createFileName(UUID disputeId, String extension) {
        return disputeId.toString() + "." + extension;
    }

    private String getFormattedAmount(DisputeParams disputeParams) {
        if (disputeParams.getCash().isEmpty()) {
            return "null";
        }

        var cash = disputeParams.getCash().get();
        var formattedAmount =
                new BigDecimal(cash.getAmount())
                        .movePointLeft(cash.getCurrency().getExponent())
                        .stripTrailingZeros()
                        .toPlainString();
        return "%s %s".formatted(formattedAmount, cash.getCurrency().getSymbolicCode());
    }

    @Override
    public DisputeStatusResult checkDisputeStatus(DisputeContext disputeContext) throws TException {
        throw new NotSupportedOperationException("Dispute must be processed manually");
    }
}