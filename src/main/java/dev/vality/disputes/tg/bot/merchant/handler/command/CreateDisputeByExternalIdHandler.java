package dev.vality.disputes.tg.bot.merchant.handler.command;

import dev.vality.disputes.tg.bot.core.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.core.service.Polyglot;
import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.core.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.merchant.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.merchant.exception.BenderException;
import dev.vality.disputes.tg.bot.merchant.handler.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.merchant.service.external.BenderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDisputeByExternalIdHandler implements MerchantMessageHandler {

    private static final String LATIN_SINGLE_LETTER = "/e ";
    private static final String CYRILLIC_SINGLE_LETTER = "/е ";
    private static final String FULL_COMMAND = "/ext ";

    private final CreateDisputeHandler createDisputeHandler;
    private final Polyglot polyglot;
    private final BenderService benderService;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(MerchantMessageDto message) {
        String messageText = extractText(message.getUpdate());
        if (messageText == null) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @SneakyThrows
    public void handle(MerchantMessageDto message) {
        log.info("Processing create dispute by externalId request");
        Update update = message.getUpdate();
        String messageText = extractText(update);
        String replyLocale = polyglot.getLocale(update);
        Optional<String> externalId = TextParsingUtil.getExternalId(messageText);
        if (externalId.isEmpty()) {
            log.warn("External id not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.external-missing");
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }

        String invoiceId;
        try {
            invoiceId = benderService.getInvoiceId(externalId.get());
        } catch (BenderException e) {
            log.warn("Unable to find external id via bender", e);
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            telegramClient.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }
        DisputeInfoDto disputeInfoDto = DisputeInfoDto.builder()
                .externalId(externalId.get())
                .invoiceId(invoiceId).build();
        telegramClient.execute(createDisputeHandler.handle(message, telegramClient, disputeInfoDto, replyLocale));
    }
}
