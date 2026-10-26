package dev.vality.disputes.tg.bot.util;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    void shouldBuildMediaGroupWithCaptionOnFirstDocument() {
        var firstStream = new ByteArrayInputStream("first".getBytes());
        var secondStream = new ByteArrayInputStream("second".getBytes());
        var firstAttachment = new InputFile(firstStream, "first.pdf");
        var secondAttachment = new InputFile(secondStream, "second.pdf");

        var response = TelegramUtil.buildTextWithDocumentsResponse(
                10L, 20, "text", List.of(firstAttachment, secondAttachment));

        assertDoesNotThrow(response::validate);
        assertEquals("10", response.getChatId());
        assertEquals(20, response.getMessageThreadId());
        assertEquals(2, response.getMedias().size());
        var firstDocument = (InputMediaDocument) response.getMedias().getFirst();
        assertEquals(firstStream, firstDocument.getNewMediaStream());
        assertEquals("first.pdf", firstDocument.getMediaName());
        assertEquals("text", firstDocument.getCaption());
        assertEquals("HTML", firstDocument.getParseMode());
        var secondDocument = (InputMediaDocument) response.getMedias().get(1);
        assertEquals(secondStream, secondDocument.getNewMediaStream());
        assertEquals("second.pdf", secondDocument.getMediaName());
        assertNull(secondDocument.getCaption());
        assertNull(secondDocument.getParseMode());
    }
}
