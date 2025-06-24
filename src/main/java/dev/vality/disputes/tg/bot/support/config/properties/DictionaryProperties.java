package dev.vality.disputes.tg.bot.support.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;


@Data
@Configuration
@ConfigurationProperties(prefix = "provider.dictionary")
public class DictionaryProperties {

    @NotNull
    private Resource file;
} 