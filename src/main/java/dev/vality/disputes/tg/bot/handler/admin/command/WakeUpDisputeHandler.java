package dev.vality.disputes.tg.bot.handler.admin.command;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.SetPendingForPoolingExpiredParams;
import dev.vality.disputes.admin.SetPendingForPoolingExpiredParamsRequest;
import dev.vality.disputes.tg.bot.handler.admin.AdminMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.WakeUpDisputeCommandParser;
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
public class WakeUpDisputeHandler implements AdminMessageHandler {

    private final AdminManagementServiceSrv.Iface adminManagementClient;
    private final TelegramApiService telegramApiService;
    private final Polyglot polyglot;
    private final WakeUpDisputeCommandParser wakeUpDisputeCommandParser;

    @Override
    public boolean filter(Update update) {
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!update.hasMessage()) {
            return false;
        }

        return wakeUpDisputeCommandParser.canParse(messageText);
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        log.info("[{}] Processing wake up dispute command from {}",
                update.getUpdateId(), TelegramUtil.extractUserInfo(update));
        String messageText = extractText(update);
        log.info("[{}] Extracted text: {}", update.getUpdateId(), messageText);

        var wakeUpDisputeCommand = wakeUpDisputeCommandParser.parse(messageText);
        if (wakeUpDisputeCommand.hasValidationError()) {
            var error = wakeUpDisputeCommand.getValidationError();
            log.warn("Command validation error: {} for command: {}", error,
                    wakeUpDisputeCommand.getClass().getSimpleName());
            String replyText = polyglot.getText(polyglot.getLocale(), error.getMessageKey());
            telegramApiService.sendReplyTo(replyText, update);
            return;
        }

        var disputeInfo = wakeUpDisputeCommand.getDisputeInfo();
        var request = new SetPendingForPoolingExpiredParamsRequest();
        request.addToSetPendingForPoolingExpiredParams(
                new SetPendingForPoolingExpiredParams(disputeInfo.getInvoiceId(), disputeInfo.getPaymentId()));
        adminManagementClient.setPendingForPoolingExpired(request);
        log.info("[{}] Wake up dispute command processed successfully for disputeInfo: {} from {}",
                update.getUpdateId(), wakeUpDisputeCommand.getDisputeInfo(), TelegramUtil.extractUserInfo(update));
        telegramApiService.setThumbUpReaction(update.getMessage().getChatId(), update.getMessage().getMessageId());
    }
}
