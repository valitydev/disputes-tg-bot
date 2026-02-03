package dev.vality.disputes.tg.bot.servlet.impl;

import dev.vality.disputes.admin.AdminCallbackServiceSrv;
import dev.vality.disputes.admin.Dispute;
import dev.vality.disputes.tg.bot.config.properties.AdminChatProperties;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.AdminDisputeReview;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.DominantService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import dev.vality.disputes.tg.bot.util.FormatUtil;
import dev.vality.disputes.tg.bot.util.InvoiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputesApiNotificationHandler implements AdminCallbackServiceSrv.Iface {

    private final AdminChatProperties adminChatProperties;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final DominantService dominantCacheService;
    private final HellgateService hellgateService;
    private final AdminDisputeReviewDao adminDisputeReviewDao;

    @Override
    public void notify(Dispute dispute) throws TException {
        if (isUsefulManualPending(dispute)) {
            log.info("Processing {} notification", dispute);
            handleManualPending(dispute);
        }
    }

    private boolean isUsefulManualPending(Dispute dispute) {
        return "manual_pending".equals(dispute.getStatus()) && !(dispute.getTechnicalErrorMessage().isPresent()
                && dispute.getTechnicalErrorMessage().get().contains("dispute via disputes-tg-bot"));
    }

    private void handleManualPending(Dispute dispute) throws TException {
        try {
            var supportMessage = getManualPendingMessage(dispute);
            var deliveredMessage = telegramApiService.sendMessage(supportMessage, adminChatProperties.getId(),
                    adminChatProperties.getTopics().getReviewDisputesProcessing());
            adminDisputeReviewDao.save(buildAdminDisputeReview(
                    Long.valueOf(deliveredMessage.orElseThrow().getMessageId()), dispute));
        } catch (Exception e) {
            log.error("Unable to save support dispute", e);
            throw e;
        }
    }

    @SneakyThrows
    private String getManualPendingMessage(Dispute dispute) {
        var invoice = hellgateService.getInvoice(dispute.getInvoiceId());
        var details = new StringJoiner("\n");
        dispute.getMapping().ifPresent(v -> details.add("mapping:" + v));
        dispute.getProviderMessage().ifPresent(v -> details.add("provider-text:" + v));
        dispute.getTechnicalErrorMessage().ifPresent(v -> details.add("error-text:" + v));
        var phraseKey =
                details.length() > 0 ? "dispute.support.manual-pending-with-text" : "dispute.support.manual-pending";
        String reply;
        var providerRef = InvoiceUtil.getProviderRef(invoice, dispute.getPaymentId());
        reply = polyglot.getText(
                phraseKey,
                "(%d) %s".formatted(providerRef.getId(), dominantCacheService.getProvider(providerRef).getName()),
                FormatUtil.formatPaymentId(dispute.getInvoiceId(), dispute.getPaymentId()),
                InvoiceUtil.getProviderTrxId(InvoiceUtil.getInvoicePayment(invoice, dispute.getPaymentId())),
                details);
        return reply;
    }

    private AdminDisputeReview buildAdminDisputeReview(Long messageId, Dispute dispute) {
        var adminDisputeReview = new AdminDisputeReview();
        adminDisputeReview.setTgMessageId(messageId);
        adminDisputeReview.setInvoiceId(dispute.getInvoiceId());
        adminDisputeReview.setPaymentId(dispute.getPaymentId());
        return adminDisputeReview;
    }

    public void notifyDisabled(Dispute dispute) throws TException {
        log.info("Processing {} notification", dispute);
        switch (dispute.getStatus()) {
            case "already_exist_created" -> {
                var reply = polyglot.getText("dispute.support.already-created",
                        FormatUtil.formatPaymentId(dispute.getInvoiceId(), dispute.getPaymentId()));
                telegramApiService.sendMessage(reply, adminChatProperties.getId(),
                        adminChatProperties.getTopics().getReviewDisputesProcessing());
            }
            case "pooling_expired" -> {
                var reply = polyglot.getText("dispute.support.pooling-expired",
                        FormatUtil.formatPaymentId(dispute.getInvoiceId(), dispute.getPaymentId()));
                telegramApiService.sendMessage(reply, adminChatProperties.getId(),
                        adminChatProperties.getTopics().getReviewDisputesProcessing());
            }
            case "create_adjustment" -> {
                var reply = polyglot.getText("dispute.support.adjustment-ready",
                        FormatUtil.formatPaymentId(dispute.getInvoiceId(), dispute.getPaymentId()));
                telegramApiService.sendMessage(reply, adminChatProperties.getId(),
                        adminChatProperties.getTopics().getReviewDisputesProcessing());
            }
            case "manual_pending" -> {
                handleManualPending(dispute);
            }
            default -> {
                if ("created".equals(dispute.getStatus())
                        || "pending".equals(dispute.getStatus())) {
                    return;
                }
                var supportMessage = getStatusChangedMessage(dispute);
                telegramApiService.sendMessage(supportMessage, adminChatProperties.getId(),
                        adminChatProperties.getTopics().getReviewDisputesProcessing());
            }
        }
    }

    @SneakyThrows
    private String getStatusChangedMessage(Dispute dispute) {
        var details = new StringJoiner("\n");
        details.add("status:" + dispute.getStatus());
        dispute.getMapping().ifPresent(v -> details.add("mapping:" + v));
        dispute.getProviderMessage().ifPresent(v -> details.add("provider-text:" + v));
        dispute.getTechnicalErrorMessage().ifPresent(v -> details.add("error-text:" + v));
        dispute.getMode().ifPresent(v -> details.add("mode:" + v));
        var invoice = hellgateService.getInvoice(dispute.getInvoiceId());
        String reply;
        var providerRef = InvoiceUtil.getProviderRef(invoice, dispute.getPaymentId());
        reply = polyglot.getText(
                "dispute.support.status-changed",
                "(%d) %s".formatted(providerRef.getId(), dominantCacheService.getProvider(providerRef).getName()),
                FormatUtil.formatPaymentId(dispute.getInvoiceId(), dispute.getPaymentId()),
                InvoiceUtil.getProviderTrxId(InvoiceUtil.getInvoicePayment(invoice, dispute.getPaymentId())),
                details);
        return reply;
    }
}
