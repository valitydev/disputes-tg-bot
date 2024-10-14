package dev.vality.disputes.tg.bot.support.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "support.chat")
public class SupportChatProperties {

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
