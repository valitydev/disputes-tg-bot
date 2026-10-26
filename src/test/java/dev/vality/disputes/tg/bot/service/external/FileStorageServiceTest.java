package dev.vality.disputes.tg.bot.service.external;

import com.sun.net.httpserver.HttpServer;
import dev.vality.disputes.admin.AttachmentRef;
import dev.vality.disputes.tg.bot.exception.FileStorageException;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @Test
    void shouldDownloadAdminAttachmentFromSourceUrl() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var data = new byte[]{1, 2, 3};
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, data.length);
            try (var output = exchange.getResponseBody()) {
                output.write(data);
            }
        });
        server.start();
        try (var http = HttpClients.createDefault()) {
            var service = new FileStorageService(http);
            var attachment = new AttachmentRef(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/file?signature=test", "application/pdf");

            var file = service.downloadAdminAttachment(attachment);

            assertArrayEquals(data, file.getContent());
            assertEquals("application/pdf", file.getMediaType().toString());
            attachment.setSourceUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/missing");
            assertThrows(FileStorageException.class, () -> service.downloadAdminAttachment(attachment));
        } finally {
            server.stop(0);
        }
    }
}
