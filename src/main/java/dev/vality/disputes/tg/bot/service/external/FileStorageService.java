package dev.vality.disputes.tg.bot.service.external;

import dev.vality.disputes.tg.bot.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final HttpClient s3HttpClient;

    public byte[] downloadFile(String downloadUrl) {
        try {
            var content = s3HttpClient.execute(new HttpGet(downloadUrl),
                    new AbstractHttpClientResponseHandler<byte[]>() {
                        @Override
                        public byte[] handleEntity(HttpEntity entity) throws IOException {
                            return EntityUtils.toByteArray(entity);
                        }
                    });
            log.info("Attachment downloaded successfully ({}kb)", content.length / 1024);
            return content;
        } catch (IOException e) {
            throw new FileStorageException("Unable to download attachment", e);
        }
    }
}
