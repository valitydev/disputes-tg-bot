package dev.vality.disputes.tg.bot.merchant.handler;

import dev.vality.disputes.tg.bot.common.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.common.dto.MessageDto;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import dev.vality.disputes.tg.bot.common.service.Polyglot;
import dev.vality.disputes.tg.bot.common.util.TelegramUtil;
import dev.vality.disputes.tg.bot.common.util.TextParsingUtil;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.merchant.service.external.BenderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static dev.vality.disputes.tg.bot.common.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtIdCreateDisputeHandler implements CommonHandler {

    private static final String LATIN_SINGLE_LETTER = "/e ";
    private static final String CYRILLIC_SINGLE_LETTER = "/е ";
    private static final String FULL_COMMAND = "/ext ";

    private final CreateDisputeHandler createDisputeHandler;
    private final Polyglot polyglot;
    private final BenderService benderService;

    @Override
    public boolean filter(MessageDto messageDto) {
        var update = messageDto.getUpdate();
        String messageText = extractText(update);
        if (messageText == null) {
            return false;
        }

        if (!ChatType.merchant.equals(messageDto.getChat().getType())) {
            return false;
        }

        return messageText.startsWith(LATIN_SINGLE_LETTER)
                || messageText.startsWith(CYRILLIC_SINGLE_LETTER)
                || messageText.startsWith(FULL_COMMAND);
    }

    @Override
    @SneakyThrows
    public void handle(MessageDto messageDto, DisputesBot disputesBot) {
        log.info("Processing create dispute by externalId request");
        Update update = messageDto.getUpdate();
        String messageText = extractText(update);
        String replyLocale = polyglot.getLocale(update);
        Optional<String> externalId = TextParsingUtil.getExternalId(messageText);
        if (externalId.isEmpty()) {
            log.warn("External id not found, message text: {}", messageText);
            String reply = polyglot.getText(replyLocale, "error.input.external-missing");
            disputesBot.execute(TelegramUtil.buildPlainTextResponse(update, reply));
            return;
        }

        String invoiceID = benderService.getInvoiceId(externalId.get());
        DisputeInfoDto disputeInfoDto = DisputeInfoDto.builder()
                .externalId(externalId.get())
                .invoiceId(invoiceID).build();
        disputesBot.execute(createDisputeHandler.handle(update, disputesBot, disputeInfoDto, replyLocale));
    }
}
