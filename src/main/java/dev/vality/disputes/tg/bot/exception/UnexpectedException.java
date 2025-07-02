package dev.vality.disputes.tg.bot.exception;

public class UnexpectedException extends RuntimeException {

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
