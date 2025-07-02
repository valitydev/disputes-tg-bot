package dev.vality.disputes.tg.bot.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponsePattern {
    public ResponseType responseType;
    public String responseText;
    public String include;
    public List<String> exclude;

    public enum ResponseType {
        @JsonProperty("approved")
        APPROVED,
        @JsonProperty("pending")
        PENDING,
        @JsonProperty("declined")
        DECLINED
    }
} 