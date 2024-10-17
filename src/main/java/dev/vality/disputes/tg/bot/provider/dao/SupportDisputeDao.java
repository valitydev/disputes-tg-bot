package dev.vality.disputes.tg.bot.provider.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.SupportDispute;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.domain.tables.SupportDispute.SUPPORT_DISPUTE;

@Component
public class SupportDisputeDao extends AbstractGenericDao {

    private final RowMapper<SupportDispute> disputeRowMapper;

    @Autowired
    public SupportDisputeDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(SUPPORT_DISPUTE, SupportDispute.class);
    }

    public boolean isReplyToDisputeMessage(Long replyToMessageId, Long chatId) {
        return getReplyToMessageId(replyToMessageId, chatId).isPresent();
    }

    public Optional<SupportDispute> getReplyToMessageId(Long replyToMessageId, Long chatId) {
        var query = getDslContext().selectFrom(SUPPORT_DISPUTE)
                .where(SUPPORT_DISPUTE.TG_MESSAGE_ID.eq(replyToMessageId).and(
                        SUPPORT_DISPUTE.CHAT_ID.eq(chatId)));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public Long save(SupportDispute dispute) {
        var record = getDslContext().newRecord(SUPPORT_DISPUTE, dispute);
        var query = getDslContext().insertInto(SUPPORT_DISPUTE)
                .set(record)
                .returning(SUPPORT_DISPUTE.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKey()).map(Number::longValue)
                .orElseThrow();
    }

    public int update(SupportDispute dispute) {
        var record = getDslContext().newRecord(SUPPORT_DISPUTE, dispute);
        var query = getDslContext().update(SUPPORT_DISPUTE)
                .set(record)
                .where(SUPPORT_DISPUTE.ID.eq(dispute.getId()));

        return execute(query);
    }
}
