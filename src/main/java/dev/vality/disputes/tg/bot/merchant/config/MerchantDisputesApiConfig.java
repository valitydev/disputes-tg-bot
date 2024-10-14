package dev.vality.disputes.tg.bot.merchant.config;

import dev.vality.disputes.merchant.MerchantDisputesServiceSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class MerchantDisputesApiConfig {

    @Bean
    public MerchantDisputesServiceSrv.Iface disputesMerchantClient(
            @Value("${service.disputes-api-merchant.url}") Resource resource,
            @Value("${service.disputes-api-merchant.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(MerchantDisputesServiceSrv.Iface.class);
    }
}
