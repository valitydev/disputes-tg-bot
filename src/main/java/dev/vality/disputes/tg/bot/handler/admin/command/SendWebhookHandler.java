package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.MerchantsNotificationParamsRequest;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.SendWebhookCommandParser;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendWebhookHandler implements AdminMessageHandler {



    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final SendWebhookCommandParser sendWebhookCommandParser;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage()) {
            return false;
        }

        return sendWebhookCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing send webhook command from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var sendWebhookCommand = sendWebhookCommandParser.parse(messageText);
        if (sendWebhookCommand.hasValidationError()) {
            var error = sendWebhookCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    sendWebhookCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        var request = new MerchantsNotificationParamsRequest();
        request.setInvoiceId(sendWebhookCommand.getDisputeInfo().getInvoiceId());
        request.setPaymentId(sendWebhookCommand.getDisputeInfo().getPaymentId());
        adminManagementClient.sendMerchantsNotification(request);
        log.info("[{}] Send webhook command processed successfully from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }

}
