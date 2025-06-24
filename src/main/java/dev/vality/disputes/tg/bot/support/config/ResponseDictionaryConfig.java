package dev.vality.disputes.tg.bot.support.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.disputes.tg.bot.support.config.properties.DictionaryProperties;
import dev.vality.disputes.tg.bot.support.service.ResponsePattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ResponseDictionaryConfig {

    private final DictionaryProperties dictionaryProperties;

    @Bean
    public List<ResponsePattern> providerResponseDictionary(ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(dictionaryProperties.getFile().getURL(), new TypeReference<>() {
        });
    }
} 