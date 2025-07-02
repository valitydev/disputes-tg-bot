package dev.vality.disputes.tg.bot.config;

import dev.vality.bender.BenderSrv;
import dev.vality.damsel.domain_config.RepositoryClientSrv;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.merchant.MerchantDisputesServiceSrv;
import dev.vality.disputes.tg.bot.config.properties.BotProperties;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;

@Configuration
public class ExternalConnectionsConfig {

    @Bean
    public BenderSrv.Iface benderClient(
            @Value("${service.bender.url}") Resource resource,
            @Value("${service.bender.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(BenderSrv.Iface.class);
    }

    @Bean
    public InvoicingSrv.Iface invoicingClient(
            @Value("${service.invoicing.url}") Resource resource,
            @Value("${service.invoicing.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(InvoicingSrv.Iface.class);
    }

    @Bean
    public RepositoryClientSrv.Iface dominantClient(
            @Value("${service.dominant.url}") Resource resource,
            @Value("${service.dominant.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI())
                .build(RepositoryClientSrv.Iface.class);
    }

    @Bean
    public AdminManagementServiceSrv.Iface adminManagementClient(
            @Value("${service.disputes-api-admin-management.url}") Resource resource,
            @Value("${service.disputes-api-admin-management.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(AdminManagementServiceSrv.Iface.class);
    }

    @Bean
    public MerchantDisputesServiceSrv.Iface disputesMerchantClient(
            @Value("${service.disputes-api-merchant.url}") Resource resource,
            @Value("${service.disputes-api-merchant.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(networkTimeout)
                .build(MerchantDisputesServiceSrv.Iface.class);
    }

    @Bean
    public CloseableHttpClient s3HttpClient() {
        return HttpClients.createDefault();
    }

    @Bean
    public TelegramClient telegramClient(BotProperties botProperties) {
        return new OkHttpTelegramClient(botProperties.getToken());
    }

}
