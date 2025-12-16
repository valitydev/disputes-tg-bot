package dev.vality.disputes.tg.bot.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "admin.chat")
public class AdminChatProperties {

    @NotNull
    private Long id;
    @NotNull
    private Topic topics;

    @Data
    public static class Topic {
        private Integer serviceInfo;
        private Integer reviewDisputesProcessing;
        private Integer approvedDisputesProcessing;
        private Integer unknownProvider;
        private Integer batchDisputesProcessing;
        private Integer chatManagement;
        private Integer reviewDisputesPatterns;
    }
}
