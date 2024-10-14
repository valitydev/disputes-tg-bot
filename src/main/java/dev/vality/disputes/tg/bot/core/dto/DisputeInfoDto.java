package dev.vality.disputes.tg.bot.core.dto;

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

}
