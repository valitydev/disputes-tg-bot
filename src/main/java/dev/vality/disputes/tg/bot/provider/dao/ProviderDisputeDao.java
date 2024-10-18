package dev.vality.disputes.tg.bot.provider.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderDispute;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.domain.tables.ProviderDispute.PROVIDER_DISPUTE;

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

    public ProviderDispute get(Long replyToMessageId, Long chatId) {
        var query = getDslContext().selectFrom(PROVIDER_DISPUTE)
                .where(PROVIDER_DISPUTE.TG_MESSAGE_ID.eq(replyToMessageId).and(
                        PROVIDER_DISPUTE.CHAT_ID.eq(chatId)));
        return fetchOne(query, disputeRowMapper);
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
}
