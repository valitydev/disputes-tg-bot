package dev.vality.disputes.tg.bot.merchant.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.common.dto.DisputeInfoDto;
import dev.vality.disputes.tg.bot.domain.enums.DisputeStatus;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantDispute;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.domain.tables.MerchantDispute.MERCHANT_DISPUTE;

@Component
public class DisputeDao extends AbstractGenericDao {

    private final RowMapper<MerchantDispute> disputeRowMapper;

    @Autowired
    public DisputeDao(DataSource dataSource) {
        super(dataSource);
        disputeRowMapper = new RecordRowMapper<>(MERCHANT_DISPUTE, MerchantDispute.class);
    }

    public Optional<MerchantDispute> getPendingDisputeByInvoiceIdAndPaymentId(DisputeInfoDto disputeInfoDto) {
        var query = getDslContext().selectFrom(MERCHANT_DISPUTE)
                .where(MERCHANT_DISPUTE.INVOICE_ID.eq(disputeInfoDto.getInvoiceId())
                        .and(MERCHANT_DISPUTE.PAYMENT_ID.eq(disputeInfoDto.getPaymentId())
                                .and(MERCHANT_DISPUTE.STATUS.eq(DisputeStatus.pending))));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public Optional<MerchantDispute> getByDisputeId(String disputeId) {
        var query = getDslContext().selectFrom(MERCHANT_DISPUTE)
                .where(MERCHANT_DISPUTE.DISPUTE_ID.eq(disputeId));
        return Optional.ofNullable(fetchOne(query, disputeRowMapper));
    }

    public List<MerchantDispute> getByInvoiceId(String invoiceId) {
        var query = getDslContext().selectFrom(MERCHANT_DISPUTE)
                .where(MERCHANT_DISPUTE.INVOICE_ID.eq(invoiceId));
        return fetch(query, disputeRowMapper);
    }

    public Long save(MerchantDispute dispute) {
        var record = getDslContext().newRecord(MERCHANT_DISPUTE, dispute);
        var query = getDslContext().insertInto(MERCHANT_DISPUTE)
                .set(record)
                .returning(MERCHANT_DISPUTE.ID);
        var keyHolder = new GeneratedKeyHolder();
        execute(query, keyHolder);
        return Optional.ofNullable(keyHolder.getKey())
                .map(Number::longValue)
                .orElseThrow();
    }

    public List<MerchantDispute> getPendingDisputesSkipLocked(int limit) {
        var query = getDslContext().selectFrom(MERCHANT_DISPUTE)
                .where(MERCHANT_DISPUTE.STATUS.eq(DisputeStatus.pending))
                .orderBy(MERCHANT_DISPUTE.UPDATED_AT)
                .limit(limit)
                .forUpdate()
                .skipLocked();
        return Optional.ofNullable(fetch(query, disputeRowMapper))
                .orElse(List.of());
    }
}
