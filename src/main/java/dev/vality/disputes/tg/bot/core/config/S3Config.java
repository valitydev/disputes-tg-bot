package dev.vality.disputes.tg.bot.core.config;


import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public CloseableHttpClient s3HttpClient() {
        return HttpClients.createDefault();
    }
}
