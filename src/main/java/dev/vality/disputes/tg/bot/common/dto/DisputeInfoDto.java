package dev.vality.disputes.tg.bot.common.dto;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Builder
public class PaymentInfoDto {

    private String invoiceId;
    private String paymentId;
    private String externalId;
    private String disputeId;


}
