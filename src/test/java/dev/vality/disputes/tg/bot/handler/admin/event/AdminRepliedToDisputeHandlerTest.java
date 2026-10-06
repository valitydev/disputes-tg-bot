package dev.vality.disputes.tg.bot.handler.admin.event;

import dev.vality.disputes.admin.AdminManagementServiceSrv;
import dev.vality.disputes.admin.CancelParamsRequest;
import dev.vality.disputes.tg.bot.dao.AdminDisputeReviewDao;
import dev.vality.disputes.tg.bot.dao.ProviderReplyDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.AdminDisputeReview;
import dev.vality.disputes.tg.bot.service.TelegramApiService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminRepliedToDisputeHandlerTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = 123L)
    void cancelsPendingWithoutProviderReply(Long providerReplyId) throws Exception {
        var reviewDao = mock(AdminDisputeReviewDao.class);
        var replyDao = mock(ProviderReplyDao.class);
        var client = mock(AdminManagementServiceSrv.Iface.class);
        var telegram = mock(TelegramApiService.class);
        var handler = new AdminRepliedToDisputeHandler(reviewDao, replyDao, client, telegram);
        var review = new AdminDisputeReview();
        review.setInvoiceId("invoice");
        review.setPaymentId("payment");
        review.setProviderReplyId(providerReplyId);
        when(reviewDao.getReplyToMessageId(10L)).thenReturn(Optional.of(review));
        var original = new Message();
        original.setMessageId(10);
        var chat = Chat.builder().id(100L).type("supergroup").build();
        var message = new Message();
        message.setChat(chat);
        message.setMessageId(11);
        message.setText("mapping");
        message.setReplyToMessage(original);
        var update = new Update();
        update.setMessage(message);

        handler.handle(update);

        var request = ArgumentCaptor.forClass(CancelParamsRequest.class);
        verify(client).cancelPending(request.capture());
        var params = request.getValue().getCancelParams().getFirst();
        assertEquals("invoice", params.getInvoiceId());
        assertEquals("payment", params.getPaymentId());
        assertEquals(Optional.of("mapping"), params.getMapping());
        assertFalse(params.isSetProviderMessage());
        if (providerReplyId == null) {
            verifyNoInteractions(replyDao);
        } else {
            verify(replyDao).getById(providerReplyId);
        }
        assertEquals(false, review.getIsApproved());
        assertNotNull(review.getRepliedAt());
        verify(reviewDao).update(review);
        verify(telegram).setThumbUpReaction(100L, 11);
    }
}
