package dev.vality.disputes.tg.bot.provider.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.disputes.tg.bot.provider.service.DictionaryEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.List;

@Configuration
public class ResponseDictionaryConfig {

    @Bean
    public List<DictionaryEntry> providerResponseDictionary(ResourcePatternResolver resourcePatternResolver,
                                                       ObjectMapper objectMapper) throws IOException {
        Resource resource = resourcePatternResolver.getResource("classpath:filter/provider/response_filter.json");
        return objectMapper.readValue(resource.getURL(), new TypeReference<>() {
        });
    }

    @Bean
    public List<DictionaryEntry> supportResponseDictionary(ResourcePatternResolver resourcePatternResolver,
                                                            ObjectMapper objectMapper) throws IOException {
        Resource resource = resourcePatternResolver.getResource("classpath:filter/support/response_filter.json");
        return objectMapper.readValue(resource.getURL(), new TypeReference<>() {
        });
    }
}
