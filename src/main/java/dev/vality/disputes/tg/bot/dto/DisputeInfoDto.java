package dev.vality.disputes.tg.bot.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DisputeInfoDto {

    private String invoiceId;
    private String paymentId;
    private String externalId;
    private String disputeId;
    private String responseText;
    private String extra;
}
