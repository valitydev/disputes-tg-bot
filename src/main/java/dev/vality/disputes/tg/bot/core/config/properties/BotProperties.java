package dev.vality.disputes.tg.bot.core.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    @NotNull
    private String token;
    @NotNull
    private String name;
}
