package dev.vality.disputes.tg.bot.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "service.file-storage")
public class FileStorageProperties {

    @NotNull
    private Resource url;
    private int networkTimeout;
    @NotNull
    private Duration downloadUrlLifetime;
}
