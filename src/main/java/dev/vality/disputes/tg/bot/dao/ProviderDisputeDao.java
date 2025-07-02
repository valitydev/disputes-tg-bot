package dev.vality.disputes.tg.bot.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderDispute;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.core.domain.tables.ProviderDispute.PROVIDER_DISPUTE;

@Component
public class ProviderDisputeDao extends AbstractGenericDao {

    private final RowMapper<ProviderDispute> disputeRowMapper;

    @Autowired
    public ProviderDisputeDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(PROVIDER_DISPUTE, ProviderDispute.class);
    }

    public boolean isReplyToDisputeMessage(Long replyToMessageId, Long chatId) {
        var query = getDslContext().selectFrom(PROVIDER_DISPUTE)
                .where(PROVIDER_DISPUTE.TG_MESSAGE_ID.eq(replyToMessageId).and(
                        PROVIDER_DISPUTE.CHAT_ID.eq(chatId)));
        var result = Optional.ofNullable(fetchOne(query, disputeRowMapper));
        return result.isPresent();
    }

    public ProviderDispute get(UUID disputeId) {
        var query = getDslContext().selectFrom(PROVIDER_DISPUTE)
                .where(PROVIDER_DISPUTE.ID.eq(disputeId));
        return fetchOne(query, disputeRowMapper);
    }

    public ProviderDispute get(Long replyToMessageId, Long chatId) {
        var query = getDslContext().selectFrom(PROVIDER_DISPUTE)
                .where(PROVIDER_DISPUTE.TG_MESSAGE_ID.eq(replyToMessageId).and(
                        PROVIDER_DISPUTE.CHAT_ID.eq(chatId)));
        return fetchOne(query, disputeRowMapper);
    }

    public List<ProviderDispute> get(String invoiceId, String paymentId) {
        var query = getDslContext().selectFrom(PROVIDER_DISPUTE)
                .where(PROVIDER_DISPUTE.INVOICE_ID.eq(invoiceId).and(
                        PROVIDER_DISPUTE.PAYMENT_ID.eq(paymentId))
                        .and(PROVIDER_DISPUTE.REASON.isNull()));
        return fetch(query, disputeRowMapper);
    }

    public void updateTgMessageId(Long messageId, UUID disputeId) {
        var query = getDslContext().update(PROVIDER_DISPUTE)
                .set(PROVIDER_DISPUTE.TG_MESSAGE_ID, messageId)
                .where(PROVIDER_DISPUTE.ID.eq(disputeId));
        executeOne(query);
    }

    public UUID save(ProviderDispute dispute) {
        var record = getDslContext().newRecord(PROVIDER_DISPUTE, dispute);
        var query = getDslContext().insertInto(PROVIDER_DISPUTE)
                .set(record)
                .returning(PROVIDER_DISPUTE.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKeyAs(UUID.class)).orElseThrow();
    }

    public void updateReason(String invoiceId, String paymentId, String reason) {
        var query = getDslContext().update(PROVIDER_DISPUTE)
                .set(PROVIDER_DISPUTE.REASON, reason)
                .where(PROVIDER_DISPUTE.INVOICE_ID.eq(invoiceId)
                        .and(PROVIDER_DISPUTE.PAYMENT_ID.eq(paymentId)
                                .and(PROVIDER_DISPUTE.REASON.isNull())));
        execute(query);
    }
}
