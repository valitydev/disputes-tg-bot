package dev.vality.disputes.tg.bot.provider.config;

import dev.vality.disputes.admin.ManualParsingServiceSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class AdminDisputesApiConfig {

    @Bean
    public ManualParsingServiceSrv.Iface manualParsingClient(
            @Value("${service.disputes-api.url}") Resource resource,
            @Value("${service.disputes-api.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(ManualParsingServiceSrv.Iface.class);
    }
}
