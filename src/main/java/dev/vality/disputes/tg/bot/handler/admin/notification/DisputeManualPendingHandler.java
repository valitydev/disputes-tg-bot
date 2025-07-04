package dev.vality.disputes.tg.bot.handler.admin.notification;

import dev.vality.disputes.admin.DisputeManualPending;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeManualPendingHandler implements NotificationHandler<DisputeManualPending> {

    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final DominantService dominantCacheService;
    private final AdminChatProperties adminChatProperties;
    private final HellgateService hellgateService;
    private final AdminDisputeReviewDao adminDisputeReviewDao;

    @Override
    @Transactional
    public void handle(DisputeManualPending notification) {
        try {
            var supportMessage = getSupportMessage(notification);
            var deliveredMessage = telegramApiService.sendMessage(supportMessage, adminChatProperties.getId(),
                    adminChatProperties.getTopics().getReviewDisputesProcessing());
            adminDisputeReviewDao.save(buildAdminDisputeReview(
                    Long.valueOf(deliveredMessage.orElseThrow().getMessageId()), notification));
        } catch (Exception e) {
            log.error("Unable to save support dispute", e);
            throw e;
        }
    }

    @SneakyThrows
    private String getSupportMessage(DisputeManualPending notification) {
        String paymentId = FormatUtil.formatPaymentId(notification.invoiceId,
                notification.getPaymentId());
        var invoice = hellgateService.getInvoice(notification.getInvoiceId());
        var providerRef = InvoiceUtil.getProviderRef(invoice, notification.getPaymentId());
        var provider = dominantCacheService.getProvider(providerRef);
        var payment = InvoiceUtil.getInvoicePayment(invoice, notification.getPaymentId());
        var providerTrxId = InvoiceUtil.getProviderTrxId(payment);
        String reply;
        if (notification.getErrorMessage().isEmpty()) {
            reply = polyglot.getText("dispute.support.manual-pending",
                    "(%d) %s".formatted(providerRef.getId(), provider.getName()),
                    paymentId,
                    providerTrxId);
        } else {
            reply = polyglot.getText("dispute.support.manual-pending-with-text",
                    "(%d) %s".formatted(providerRef.getId(), provider.getName()),
                    paymentId,
                    providerTrxId,
                    notification.getErrorMessage().get());
        }
        return reply;
    }

    private AdminDisputeReview buildAdminDisputeReview(Long messageId,
                                                           DisputeManualPending notification) {
        AdminDisputeReview adminDisputeReview = new AdminDisputeReview();
        adminDisputeReview.setTgMessageId(messageId);
        adminDisputeReview.setInvoiceId(notification.getInvoiceId());
        adminDisputeReview.setPaymentId(notification.getPaymentId());
        return adminDisputeReview;
    }
}
