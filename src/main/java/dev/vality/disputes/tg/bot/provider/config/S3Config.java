package dev.vality.disputes.tg.bot.provider.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public CloseableHttpClient s3HttpClient() {
        return HttpClients.createDefault();
    }
}
