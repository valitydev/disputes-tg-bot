package dev.vality.disputes.tg.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TelegramUtilTest {

    @Test
    void shouldEscapeHtmlSpecialCharacters() {
        assertEquals("&lt;&gt;_Dispute &amp; test", TelegramUtil.escapeHtml("<>_Dispute & test"));
    }

    @Test
    void shouldReturnNullWhenEscapingNull() {
        assertNull(TelegramUtil.escapeHtml(null));
    }
}
