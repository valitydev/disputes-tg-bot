package dev.vality.disputes.tg.bot.support.handler.command;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.MerchantsNotificationParamsRequest;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.support.handler.SupportMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendWebhookHandler implements SupportMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/n ";
    private static final String FULL_COMMAND = "/notify ";

    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage()) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing send webhook command from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        var disputeInfo = TextParsingUtil.getDisputeInfo(messageText);
        if (disputeInfo.isEmpty()) {
            log.warn("Unable to process command, dispute info not found, command text: '{}'", messageText);
            return;
        }
        var request = new MerchantsNotificationParamsRequest();
        request.setInvoiceId(disputeInfo.get().getInvoiceId());
        request.setPaymentId(disputeInfo.get().getPaymentId());
        adminManagementClient.sendMerchantsNotification(request);
        log.info("[{}] Send webhook command processed successfully from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        var successReaction = TelegramUtil.getSetMessageReaction(update.getMessage().getChatId(),
                update.getMessage().getMessageId(), "👍");
        telegramClient.execute(successReaction);
    }

}
