package dev.vality.disputes.tg.bot.resource;

import tools.jackson.databind.ObjectMapper;
import dev.vality.disputes.tg.bot.dao.MerchantChatDao;
import dev.vality.disputes.tg.bot.dao.MerchantDisputeDao;
import dev.vality.disputes.tg.bot.dao.ProviderDisputeDao;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantChat;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.disputes.tg.bot.handler.merchant.command.StatusDisputeHandler;
import dev.vality.disputes.tg.bot.service.Polyglot;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import dev.vality.disputes.tg.bot.service.TelegramNotificationService;
import dev.vality.disputes.tg.bot.service.external.HellgateService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisputeNotificationControllerTest {

    @Test
    void checksAuthoritativeStatusAndAlwaysAcknowledges() throws Exception {
        final var disputeDao = mock(MerchantDisputeDao.class);
        var chatDao = mock(MerchantChatDao.class);
        final var statusHandler = mock(StatusDisputeHandler.class);
        var polyglot = mock(Polyglot.class);
        var telegram = mock(TelegramApiService.class);
        final var notifications = new TelegramNotificationService(polyglot, telegram, mock(ProviderDisputeDao.class),
                mock(HellgateService.class), chatDao);
        var disputeId = UUID.randomUUID();
        var dispute = new MerchantDispute();
        dispute.setId(disputeId);
        dispute.setInvoiceId("invoice");
        dispute.setPaymentId("payment");
        dispute.setStatus(DisputeStatus.pending);
        dispute.setChatId(10L);
        dispute.setTgMessageId(20L);
        var chat = new MerchantChat();
        chat.setChatId(30L);
        chat.setLocale("en");
        when(disputeDao.getByDisputeId(disputeId.toString())).thenReturn(Optional.of(dispute));
        when(chatDao.getById(10L)).thenReturn(Optional.of(chat));
        when(polyglot.getLocale("en")).thenReturn(Locale.ENGLISH);
        when(polyglot.getText(eq(Locale.ENGLISH), eq("dispute.status"), any(), any()))
                .thenReturn("approved");
        doAnswer(invocation -> {
            dispute.setStatus(DisputeStatus.approved);
            return null;
        }).when(statusHandler).updateDisputesStatuses(anyList());

        var controller = new DisputeNotificationController(new ObjectMapper(), disputeDao,
                statusHandler, notifications);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var body = """
                {"disputeId":"%s","invoiceId":"invoice","paymentId":"payment","status":"failed"}
                """.formatted(disputeId);
        mvc.perform(post("/v1/disputes/notifications").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/disputes/notifications").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/disputes/notifications").contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/disputes/notifications").contentType(APPLICATION_JSON)
                        .content(body.replace("\"paymentId\":\"payment\"", "\"paymentId\":\"other\"")))
                .andExpect(status().isOk());

        dispute.setStatus(DisputeStatus.pending);
        when(chatDao.getById(10L)).thenReturn(Optional.empty());
        mvc.perform(post("/v1/disputes/notifications").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertEquals(DisputeStatus.approved, dispute.getStatus());
        verify(statusHandler, times(3)).updateDisputesStatuses(List.of(dispute));
        verify(telegram).sendReplyTo("approved", 30L, 20);
        verifyNoMoreInteractions(telegram);
    }
}
