package dev.vality.disputes.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.domain.enums.DisputeStatus;
import dev.vality.disputes.domain.tables.pojos.Dispute;
import dev.vality.disputes.exception.NotFoundException;
import dev.vality.mapper.RecordRowMapper;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.domain.tables.Dispute.DISPUTE;

@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class DisputeDao extends AbstractGenericDao {

    private final RowMapper<Dispute> disputeRowMapper;

    @Autowired
    public DisputeDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(DISPUTE, Dispute.class);
    }

    public Long save(Dispute dispute) {
        var record = getDslContext().newRecord(DISPUTE, dispute);
        var query = getDslContext().insertInto(DISPUTE)
                .set(record)
                .returning(DISPUTE.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKey())
                .map(Number::longValue)
                .orElseThrow();
    }

    public Dispute get(long disputeId, String invoiceId, String paymentId) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.ID.eq(disputeId)
                        // мы не можем позволить получить несанкционированный доступ к данным, ограничившись disputeId
                        .and(DISPUTE.INVOICE_ID.eq(invoiceId))
                        .and(DISPUTE.PAYMENT_ID.eq(paymentId)));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format("Dispute not found, disputeId='%s', invoiceId='%s', paymentId='%s'", disputeId, invoiceId, paymentId)));
    }

    public List<Dispute> get(String invoiceId, String paymentId) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.INVOICE_ID.eq(invoiceId)
                        .and(DISPUTE.PAYMENT_ID.eq(paymentId)));
        return Optional.ofNullable(fetch(query, disputeRowMapper))
                .orElse(List.of());
    }

    public Optional<Dispute> get(long disputeId) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.ID.eq(disputeId));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public List<Dispute> getDisputesForUpdateSkipLocked(int limit, DisputeStatus disputeStatus) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.STATUS.eq(disputeStatus)
                        .and(DISPUTE.NEXT_CHECK_AFTER.le(LocalDateTime.now(ZoneOffset.UTC))))
                .orderBy(DISPUTE.NEXT_CHECK_AFTER)
                .limit(limit)
                .forUpdate()
                .skipLocked();
        return Optional.ofNullable(fetch(query, disputeRowMapper))
                .orElse(List.of());
    }

    public List<Dispute> getDisputesForHgCall(int limit) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.STATUS.eq(DisputeStatus.create_adjustment)
                        .and(DISPUTE.SKIP_CALL_HG_FOR_CREATE_ADJUSTMENT.eq(false)))
                .orderBy(DISPUTE.NEXT_CHECK_AFTER)
                .limit(limit)
                .forUpdate()
                .skipLocked();
        return Optional.ofNullable(fetch(query, disputeRowMapper))
                .orElse(List.of());
    }

    public List<Dispute> getReadyDisputesForCreateAdjustment(int limit) {
        var query = getDslContext().selectCount()selectFrom(DISPUTE)
                .where(DISPUTE.STATUS.eq(DisputeStatus.create_adjustment)
                        .and(DISPUTE.SKIP_CALL_HG_FOR_CREATE_ADJUSTMENT.eq(true)))
                .orderBy(DISPUTE.NEXT_CHECK_AFTER)
                .limit(limit)
                .forUpdate()
                .skipLocked();
        return Optional.ofNullable(fetch(query, disputeRowMapper))
                .orElse(List.of());
    }

    @Nullable
    public Dispute getDisputeForUpdateSkipLocked(long disputeId) {
        var query = getDslContext().selectFrom(DISPUTE)
                .where(DISPUTE.ID.eq(disputeId))
                .forUpdate()
                .skipLocked();
        return fetchOne(query, disputeRowMapper);
    }

    public long update(long disputeId, DisputeStatus status) {
        return update(disputeId, status, null, null, null, null);
    }

    public long update(long disputeId, DisputeStatus status, LocalDateTime nextCheckAfter) {
        return update(disputeId, status, nextCheckAfter, null, null, null);
    }

    public long update(long disputeId, DisputeStatus status, String errorMessage) {
        return update(disputeId, status, null, errorMessage, null, null);
    }

    public long update(long disputeId, DisputeStatus status, Long changedAmount) {
        return update(disputeId, status, null, null, changedAmount, null);
    }

    public long update(long disputeId, DisputeStatus status, Long changedAmount, Boolean skipCallHgForCreateAdjustment) {
        return update(disputeId, status, null, null, changedAmount, skipCallHgForCreateAdjustment);
    }

    private long update(long disputeId, DisputeStatus status, LocalDateTime nextCheckAfter, String errorMessage, Long changedAmount, Boolean skipCallHgForCreateAdjustment) {
        var set = getDslContext().update(DISPUTE)
                .set(DISPUTE.STATUS, status);
        if (nextCheckAfter != null) {
            set = set.set(DISPUTE.NEXT_CHECK_AFTER, nextCheckAfter);
        }
        if (errorMessage != null) {
            set = set.set(DISPUTE.ERROR_MESSAGE, errorMessage);
        }
        if (changedAmount != null) {
            set = set.set(DISPUTE.CHANGED_AMOUNT, changedAmount);
        }
        if (skipCallHgForCreateAdjustment != null) {
            set = set.set(DISPUTE.SKIP_CALL_HG_FOR_CREATE_ADJUSTMENT, skipCallHgForCreateAdjustment);
        }
        var query = set
                .where(DISPUTE.ID.eq(disputeId));
        executeOne(query);
        return disputeId;
    }
}
