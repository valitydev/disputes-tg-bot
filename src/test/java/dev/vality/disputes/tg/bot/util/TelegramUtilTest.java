package dev.vality.disputes.tg.bot.util;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.InputFile;

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

    @Test
    void shouldBuildDocumentReply() {
        var attachment = new InputFile("file-id");

        var response = TelegramUtil.buildDocumentReply(10L, 20, 30, attachment);

        assertEquals("10", response.getChatId());
        assertEquals(20, response.getMessageThreadId());
        assertEquals(30, response.getReplyToMessageId());
        assertEquals(attachment, response.getDocument());
        assertNull(response.getCaption());
    }
}
