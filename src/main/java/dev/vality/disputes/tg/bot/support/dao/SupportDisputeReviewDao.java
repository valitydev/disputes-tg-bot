package dev.vality.disputes.tg.bot.support.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.SupportDisputeReview;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.domain.tables.SupportDisputeReview.SUPPORT_DISPUTE_REVIEW;

@Component
public class SupportDisputeReviewDao extends AbstractGenericDao {

    private final RowMapper<SupportDisputeReview> disputeRowMapper;

    @Autowired
    public SupportDisputeReviewDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(SUPPORT_DISPUTE_REVIEW, SupportDisputeReview.class);
    }

    public boolean isReplyToDisputeMessage(Long replyToMessageId, Long chatId) {
        return getReplyToMessageId(replyToMessageId, chatId).isPresent();
    }

    public Optional<SupportDisputeReview> getReplyToMessageId(Long replyToMessageId, Long chatId) {
        //TODO: Доработать логику запроса
        var query = getDslContext().selectFrom(SUPPORT_DISPUTE_REVIEW)
                .where(SUPPORT_DISPUTE_REVIEW.TG_MESSAGE_ID.eq(replyToMessageId));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public Long save(SupportDisputeReview dispute) {
        var record = getDslContext().newRecord(SUPPORT_DISPUTE_REVIEW, dispute);
        var query = getDslContext().insertInto(SUPPORT_DISPUTE_REVIEW)
                .set(record)
                .returning(SUPPORT_DISPUTE_REVIEW.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKey()).map(Number::longValue)
                .orElseThrow();
    }

    public int update(SupportDisputeReview dispute) {
        var record = getDslContext().newRecord(SUPPORT_DISPUTE_REVIEW, dispute);
        var query = getDslContext().update(SUPPORT_DISPUTE_REVIEW)
                .set(record)
                .where(SUPPORT_DISPUTE_REVIEW.ID.eq(dispute.getId()));

        return execute(query);
    }
} 