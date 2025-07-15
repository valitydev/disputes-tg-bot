package dev.vality.disputes.tg.bot.dto.command.error;

import lombok.Getter;

/**
 * Типы ошибок валидации команд
 */
@Getter
public enum CommandValidationError {
    
    /**
     * Неверное количество аргументов
     */
    ARGUMENT_NUMBER_MISMATCH("error.input.invalid-command-format"),
    
    /**
     * Неверный формат Chat ID
     */
    INVALID_CHAT_ID("error.input.invalid-chat-id"),
    
    /**
     * Чат не найден в Telegram
     */
    CHAT_NOT_FOUND_IN_TELEGRAM("error.chat.not-found"),
    
    /**
     * Чат не найден в базе данных
     */
    CHAT_NOT_FOUND_IN_DATABASE("error.chat.not-found"),
    
    /**
     * Неверный формат Party ID
     */
    INVALID_PARTY_ID("error.input.invalid-party-id"),
    
    /**
     * Неверный формат Invoice ID
     */
    INVALID_INVOICE_ID("error.input.invalid-invoice-id"),
    
    /**
     * Неверный формат Payment ID
     */
    INVALID_PAYMENT_ID("error.input.invalid-payment-id"),
    
    /**
     * Неверный формат Provider ID
     */
    INVALID_PROVIDER_ID("error.input.invalid-provider-id"),
    
    /**
     * Неверный формат External ID
     */
    INVALID_EXTERNAL_ID("error.input.invalid-external-id"),
    
    /**
     * Неверный формат числа
     */
    INVALID_NUMBER_FORMAT("error.input.invalid-number-format"),
    
    /**
     * Неверный формат даты
     */
    INVALID_DATE_FORMAT("error.input.invalid-date-format"),
    
    /**
     * Неверный формат команды
     */
    INVALID_COMMAND_FORMAT("error.input.invalid-command-format"),
    
    /**
     * Неизвестная ошибка
     */
    UNKNOWN_ERROR("error.input.unknown-error");

    /**
     * -- GETTER --
     *  Получает ключ сообщения для перевода
     *
     * @return ключ сообщения
     */
    private final String messageKey;
    
    CommandValidationError(String messageKey) {
        this.messageKey = messageKey;
    }

} 