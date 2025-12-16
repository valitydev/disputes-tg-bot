package dev.vality.disputes.tg.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextParsingUtilTest {

    @Test
    void invoiceParsingTest() {
        var message = """
                @ArbitrageC2C_Bot
                1235.28PZQGlfttg.12
                12050
                
                #28PZQGlfttg
                """;
        var disputeInfoDto = TextParsingUtil.getDisputeInfo(message).get();
        assertEquals("28PZQGlfttg", disputeInfoDto.getInvoiceId());
        assertEquals("12", disputeInfoDto.getPaymentId());
    }
}
