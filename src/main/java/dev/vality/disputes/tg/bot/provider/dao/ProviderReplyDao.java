package dev.vality.disputes.tg.bot.provider.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderReply;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.core.domain.Tables.PROVIDER_REPLY;


@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class ProviderReplyDao extends AbstractGenericDao {

    private final RowMapper<ProviderReply> providerReplyRowMapper;

    public ProviderReplyDao(DataSource dataSource) {
        super(dataSource);
        providerReplyRowMapper = new RecordRowMapper<>(PROVIDER_REPLY, ProviderReply.class);
    }

    public void save(ProviderReply providerReply) {
        var record = getDslContext().newRecord(PROVIDER_REPLY, providerReply);
        var query = getDslContext().insertInto(PROVIDER_REPLY)
                .set(record);
        executeOne(query);
    }

    public List<ProviderReply> getAllReplies(UUID disputeId) {
        var query = getDslContext().selectFrom(PROVIDER_REPLY)
                .where(PROVIDER_REPLY.DISPUTE_ID.eq(disputeId))
                .orderBy(PROVIDER_REPLY.REPLIED_AT.asc());
        return fetch(query, providerReplyRowMapper);
    }
}
