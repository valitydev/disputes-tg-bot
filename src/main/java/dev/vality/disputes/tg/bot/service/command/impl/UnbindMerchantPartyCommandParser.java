package dev.vality.disputes.tg.bot.service.command.impl;

import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dto.command.UnbindMerchantPartyCommand;
import dev.vality.disputes.tg.bot.dto.command.UnbindPartyType;
import dev.vality.disputes.tg.bot.dto.command.error.CommandValidationError;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.command.CommandParser;
import dev.vality.disputes.tg.bot.util.CommandValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnbindMerchantPartyCommandParser implements CommandParser<UnbindMerchantPartyCommand> {

    private static final String LATIN_SINGLE_LETTER = "/ub ";
    private static final String FULL_COMMAND = "/unbind ";

    private final MerchantChatDao merchantChatDao;
    private final TelegramApiService telegramApiService;

    @Override
    public UnbindMerchantPartyCommand parse(@NonNull String messageText) {
        if (!CommandValidationUtil.hasExpectedArgsCount(messageText, 2)) {
            return UnbindMerchantPartyCommand.builder()
                    .validationError(CommandValidationError.ARGUMENT_NUMBER_MISMATCH)
                    .build();
        }

        String[] args = CommandValidationUtil.extractArgs(messageText);
        Optional<Long> chatIdOpt = CommandValidationUtil.extractLong(args[0], "Chat ID");

        if (chatIdOpt.isEmpty()) {
            return UnbindMerchantPartyCommand.builder()
                    .validationError(CommandValidationError.INVALID_CHAT_ID)
                    .build();
        }

        // Проверяем существование чата в Telegram
        if (telegramApiService.getChatInfo(chatIdOpt.get()).isEmpty()) {
            log.warn("Chat not found in Telegram: {}", chatIdOpt.get());
            return UnbindMerchantPartyCommand.builder()
                    .validationError(CommandValidationError.CHAT_NOT_FOUND_IN_TELEGRAM)
                    .build();
        }

        // Получаем MerchantChat из базы данных
        var merchantChat = merchantChatDao.get(chatIdOpt.get());
        if (merchantChat.isEmpty()) {
            log.warn("Merchant chat not found for chat ID: {}", chatIdOpt.get());
            return UnbindMerchantPartyCommand.builder()
                    .validationError(CommandValidationError.CHAT_NOT_FOUND_IN_DATABASE)
                    .build();
        }

        String partyId = args[1];
        UnbindPartyType unbindType = UnbindPartyType.fromString(partyId);
        
        return UnbindMerchantPartyCommand.builder()
                .chatId(chatIdOpt.get())
                .partyId(partyId)
                .unbindType(unbindType)
                .merchantChat(merchantChat.get())
                .build();
    }

    @Override
    public boolean canParse(@NonNull String commandString) {
        return commandString.startsWith(LATIN_SINGLE_LETTER) || commandString.startsWith(FULL_COMMAND);
    }

} 