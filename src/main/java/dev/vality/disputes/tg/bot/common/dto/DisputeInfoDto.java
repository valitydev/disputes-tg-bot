package dev.vality.disputes.tg.bot.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Builder
public class DisputeInfoDto {

    private String invoiceId;
    private String paymentId;
    private String externalId;
    private String disputeId;


}
