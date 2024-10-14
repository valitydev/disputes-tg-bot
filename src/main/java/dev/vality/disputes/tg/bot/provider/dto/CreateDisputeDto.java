package dev.vality.disputes.tg.bot.provider.dto;

import dev.vality.disputes.provider.DisputeParams;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

@Builder
@Getter
@Setter
public class CreateDisputeDto {
    private DisputeParams disputeParams;
    private Document attachment;

    @Data
    public static class Document {
        private byte[] content;
        private MediaType mediaType;
    }
}
