package dev.vality.disputes.tg.bot.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolyglotTest {

    @Test
    void shouldEscapeTextArgumentsAndKeepTemplateHtmlTags() {
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("test.message", Locale.of("ru"), "Value: <code>{0}</code>");
        var polyglot = new Polyglot(messageSource);

        var text = polyglot.getText("test.message", "<>pay_Dispute & test");

        assertEquals("Value: <code>&lt;&gt;pay_Dispute &amp; test</code>", text);
    }

    @Test
    void shouldEscapeNonStringArgumentsByStringRepresentation() {
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("test.message", Locale.of("ru"), "Details: <code>{0}</code>");
        var polyglot = new Polyglot(messageSource);
        var details = new StringJoiner("\n").add("provider-text:<>pay_Dispute");

        var text = polyglot.getText("test.message", details);

        assertEquals("Details: <code>provider-text:&lt;&gt;pay_Dispute</code>", text);
    }
}
