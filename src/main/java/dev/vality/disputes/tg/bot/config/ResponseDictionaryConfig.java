package dev.vality.disputes.tg.bot.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.disputes.tg.bot.config.model.ResponsePattern;
import dev.vality.disputes.tg.bot.config.properties.DictionaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

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

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource msgSrc = new ReloadableResourceBundleMessageSource();
        msgSrc.setBasename("classpath:i18n/messages");
        msgSrc.setDefaultEncoding("UTF-8");
        msgSrc.setFallbackToSystemLocale(false);
        return msgSrc;
    }
} 