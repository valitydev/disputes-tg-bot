package dev.vality.disputes.tg.bot.handler.merchant.command;

import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantPartyDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantParty;
import dev.vality.disputes.tg.bot.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.dto.MerchantMessageDto;
import dev.vality.disputes.tg.bot.dto.command.CreateDisputeByExternalIdCommand;
import dev.vality.disputes.tg.bot.exception.BenderException;
import dev.vality.disputes.tg.bot.handler.merchant.MerchantMessageHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.impl.CreateDisputeByExternalIdCommandParser;
import dev.vality.disputes.tg.bot.service.external.BenderService;
import dev.vality.disputes.tg.bot.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;

import static dev.vality.disputes.tg.bot.util.TelegramUtil.extractText;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDisputeByExternalIdHandler implements MerchantMessageHandler {

    private final CreateDisputeHandler createDisputeHandler;
    private final CreateDisputeByExternalIdCommandParser commandParser;
    private final Polyglot polyglot;
    private final BenderService benderService;
    private final TelegramApiService telegramApiService;
    private final MerchantChatDao merchantChatDao;
    private final MerchantPartyDao merchantPartyDao;

    @Override
    public boolean filter(MerchantMessageDto message) {
        String messageText = extractText(message.getUpdate());
        return commandParser.canParse(messageText);
    }

    @Override
    @Transactional
    public void handle(MerchantMessageDto message) {
        log.info("Processing create dispute by externalId request");
        Update update = message.getUpdate();
        String messageText = extractText(update);
        Locale replyLocale = polyglot.getLocale();
        CreateDisputeByExternalIdCommand command = commandParser.parse(messageText);
        if (command.hasValidationError()) {
            sendErrorMessageToUser(command, replyLocale, update);
            return;
        }
        String invoiceId = findInvoiceIdByPartyIds(update, command.getExternalId(), replyLocale);
        if (invoiceId == null) {
            return; // Ошибка уже обработана в findInvoiceIdByPartyIds
        }

        DisputeInfoDto disputeInfoDto = DisputeInfoDto.builder()
                .externalId(command.getExternalId())
                .invoiceId(invoiceId).build();
        createDisputeHandler.handle(message, disputeInfoDto, replyLocale);
    }

    private void sendErrorMessageToUser(CreateDisputeByExternalIdCommand command, Locale replyLocale, Update update) {
        log.warn("Command validation failed: {}", command.getValidationError().getMessageKey());
        String reply = polyglot.getText(replyLocale, command.getValidationError().getMessageKey());
        telegramApiService.sendReplyTo(reply, update);
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
