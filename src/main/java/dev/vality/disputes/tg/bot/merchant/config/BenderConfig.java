package dev.vality.disputes.tg.bot.merchant.config;

import dev.vality.bender.BenderSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class BenderConfig {

    @Bean
    public BenderSrv.Iface benderClient(
            @Value("${service.bender.url}") Resource resource,
            @Value("${service.bender.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(BenderSrv.Iface.class);
    }

}
