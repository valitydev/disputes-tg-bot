package dev.vality.disputes.tg.bot.dto;

import lombok.Data;
import org.springframework.http.MediaType;

@Data
public class File {
    private byte[] content;
    private MediaType mediaType;
}
