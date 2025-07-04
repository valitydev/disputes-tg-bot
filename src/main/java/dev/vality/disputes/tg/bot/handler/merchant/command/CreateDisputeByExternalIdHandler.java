package dev.vality.disputes.tg.bot.handler.merchant.command;

import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantParty;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.exception.BenderException;
import dev.vality.disputes.tg.bot.handler.merchant.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.external.BenderService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import dev.vality.disputes.tg.bot.util.TextParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

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
    private final TelegramApiService telegramApiService;
    private final MerchantChatDao merchantChatDao;
    private final MerchantPartyDao merchantPartyDao;

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
    public void handle(MerchantMessageDto message) {
        log.info("Processing create dispute by externalId request");
        Update update = message.getUpdate();
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale();
        Optional<String> externalId = TextParsingUtil.getExternalId(messageText);
        if (externalId.isEmpty()) {
            log.warn("External id not found, message text: {}", messageText);
            String reply = polyglot.getText(polyglot.getLocale(), "error.input.external-missing");
            telegramApiService.sendReplyTo(reply, update);
            return;
        }

        String invoiceId = findInvoiceIdByPartyIds(update, externalId.get(), replyLocale);
        if (invoiceId == null) {
            return; // Ошибка уже обработана в findInvoiceIdByPartyIds
        }

        DisputeInfoDto disputeInfoDto = DisputeInfoDto.builder()
                .externalId(externalId.get())
                .invoiceId(invoiceId).build();
        createDisputeHandler.handle(message, disputeInfoDto, replyLocale);
    }

    private String findInvoiceIdByPartyIds(Update update, String externalId, Locale replyLocale) {
        // Получаем chat_id из сообщения
        Long chatId = TelegramUtil.getChatId(update);
        if (chatId == null) {
            log.error("Unable to get chat ID from update");
            return null;
        }

        // Получаем merchant_chat из БД
        var merchantChat = merchantChatDao.get(chatId);
        if (merchantChat.isEmpty()) {
            log.warn("Merchant chat not found for chat ID: {}", chatId);
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            telegramApiService.sendReplyTo(reply, update);
            return null;
        }

        // Получаем все party_id, привязанные к этому чату
        List<MerchantParty> merchantParties = 
                merchantPartyDao.getAllByMerchantChatId(merchantChat.get().getId());
        
        if (merchantParties.isEmpty()) {
            log.info("No partyIds found for chat ID: {}", chatId);
            String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
            telegramApiService.sendReplyTo(reply, update);
            return null;
        }

        // Перебираем все party_id и пытаемся найти invoice_id
        for (var merchantParty : merchantParties) {
            String partyId = merchantParty.getPartyId();
            
            try {
                String invoiceId = benderService.getInvoiceId(partyId, externalId);
                log.info("Successfully found invoice ID: {} for party ID: {} and external ID: {}", 
                        invoiceId, partyId, externalId);
                return invoiceId;
            } catch (BenderException e) {
                log.debug("Failed to get invoice ID for party ID: {} and external ID: {}, trying next...", 
                        partyId, externalId);
                // Продолжаем с следующим party_id
            }
        }

        // Если ни один party_id не дал результата
        log.warn("Unable to find invoice ID via Bender for any party ID. External ID: {}, Party IDs tried: {}", 
                externalId, merchantParties.stream().map(MerchantParty::getPartyId).toList());
        String reply = polyglot.getText(replyLocale, "error.processing.invoice-not-found");
        telegramApiService.sendReplyTo(reply, update);
        return null;
    }
}
