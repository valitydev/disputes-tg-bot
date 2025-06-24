package dev.vality.disputes.tg.bot.support.handler.notification;

import dev.vality.dao.DaoException;
import dev.vality.disputes.admin.DisputeManualPending;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.SupportDisputeReview;
import dev.vality.disputes.tg.bot.core.service.DominantCacheServiceImpl;
import dev.vality.disputes.tg.bot.core.service.HellgateService;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.InvoiceUtil;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.dao.SupportDisputeReviewDao;
import dev.vality.disputes.tg.bot.core.util.FormatUtil;
import dev.vality.disputes.tg.bot.support.config.properties.SupportChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeManualPendingHandler implements NotificationHandler<DisputeManualPending> {

    private final TelegramClient telegramClient;
    private final Polyglot polyglot;
    private final DominantCacheServiceImpl dominantCacheService;
    private final SupportChatProperties supportChatProperties;
    private final HellgateService hellgateService;
    private final SupportDisputeReviewDao supportDisputeReviewDao;

    @Override
    @SneakyThrows
    public void handle(DisputeManualPending notification) {
        var supportMessage = getSupportMessage(notification);
        Message deliveredMessage = telegramClient.execute(supportMessage);
        try {
            supportDisputeReviewDao.save(buildSupportDisputeReview(deliveredMessage, notification));
        } catch (DaoException e) {
            log.error("Unable to save support dispute", e);
            throw e;
        }
    }


    // TODO: mostly copied from ProviderRepliedToDisputeHandler, unify if possible
    @SneakyThrows
    private SendMessage getSupportMessage(DisputeManualPending notification) {
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
        var tgMessage = TelegramUtil.buildPlainTextResponse(supportChatProperties.getId(), reply);
        tgMessage.setMessageThreadId(supportChatProperties.getTopics().getReviewDisputesProcessing());
        return tgMessage;
    }

    private SupportDisputeReview buildSupportDisputeReview(Message deliveredMessage,
                                                           DisputeManualPending notification) {
        SupportDisputeReview supportDisputeReview = new SupportDisputeReview();
        supportDisputeReview.setTgMessageId(Long.valueOf(deliveredMessage.getMessageId()));
        supportDisputeReview.setInvoiceId(notification.getInvoiceId());
        supportDisputeReview.setPaymentId(notification.getPaymentId());
        return supportDisputeReview;
    }
}
