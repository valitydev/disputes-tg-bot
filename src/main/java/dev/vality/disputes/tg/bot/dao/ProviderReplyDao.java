package dev.vality.disputes.tg.bot.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderReply;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static dev.vality.disputes.tg.bot.domain.Tables.PROVIDER_REPLY;


@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class ProviderReplyDao extends AbstractGenericDao {

    private final RowMapper<ProviderReply> providerReplyRowMapper;

    public ProviderReplyDao(DataSource dataSource) {
        super(dataSource);
        providerReplyRowMapper = new RecordRowMapper<>(PROVIDER_REPLY, ProviderReply.class);
    }

    public Long save(ProviderReply providerReply) {
        var record = getDslContext().newRecord(PROVIDER_REPLY, providerReply);
        var query = getDslContext().insertInto(PROVIDER_REPLY)
                .set(record)
                .returning(PROVIDER_REPLY.ID);
        var result = fetchOne(query, providerReplyRowMapper);
        return result.getId();
    }

    public ProviderReply getById(Long replyId) {
        var query = getDslContext().selectFrom(PROVIDER_REPLY)
                .where(PROVIDER_REPLY.ID.eq(replyId));
        return fetchOne(query, providerReplyRowMapper);
    }
}
