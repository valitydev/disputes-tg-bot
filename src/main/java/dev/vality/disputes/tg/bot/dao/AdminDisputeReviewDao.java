package dev.vality.disputes.tg.bot.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.AdminDisputeReview;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.domain.tables.AdminDisputeReview.ADMIN_DISPUTE_REVIEW;

@Component
public class AdminDisputeReviewDao extends AbstractGenericDao {

    private final RowMapper<AdminDisputeReview> disputeRowMapper;

    @Autowired
    public AdminDisputeReviewDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(ADMIN_DISPUTE_REVIEW, AdminDisputeReview.class);
    }

    public boolean isReplyToDisputeMessage(Long replyToMessageId) {
        return getReplyToMessageId(replyToMessageId).isPresent();
    }

    public Optional<AdminDisputeReview> getReplyToMessageId(Long replyToMessageId) {
        var query = getDslContext().selectFrom(ADMIN_DISPUTE_REVIEW)
                .where(ADMIN_DISPUTE_REVIEW.TG_MESSAGE_ID.eq(replyToMessageId));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public Long save(AdminDisputeReview dispute) {
        var record = getDslContext().newRecord(ADMIN_DISPUTE_REVIEW, dispute);
        var query = getDslContext().insertInto(ADMIN_DISPUTE_REVIEW)
                .set(record)
                .returning(ADMIN_DISPUTE_REVIEW.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKey()).map(Number::longValue)
                .orElseThrow();
    }

    public int update(AdminDisputeReview dispute) {
        var record = getDslContext().newRecord(ADMIN_DISPUTE_REVIEW, dispute);
        var query = getDslContext().update(ADMIN_DISPUTE_REVIEW)
                .set(record)
                .where(ADMIN_DISPUTE_REVIEW.ID.eq(dispute.getId()));

        return execute(query);
    }
} 