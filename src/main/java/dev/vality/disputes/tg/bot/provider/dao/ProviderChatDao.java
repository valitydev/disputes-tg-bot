package dev.vality.disputes.tg.bot.provider.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.ProviderChat;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.domain.Tables.PROVIDER_CHAT;


@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class ProviderChatDao extends AbstractGenericDao {

    private final RowMapper<ProviderChat> providerChatRowMapper;

    @Autowired
    public ProviderChatDao(DataSource dataSource) {
        super(dataSource);
        providerChatRowMapper = new RecordRowMapper<>(PROVIDER_CHAT, ProviderChat.class);
    }

    //TODO: Метод для треда
    public Optional<ProviderChat> get(long id) {
        var query = getDslContext().selectFrom(PROVIDER_CHAT)
                .where(PROVIDER_CHAT.READ_FROM_CHAT_ID.eq(id).and(PROVIDER_CHAT.ENABLED));
        return Optional.ofNullable(fetchOne(query, providerChatRowMapper));
    }

    public Optional<ProviderChat> getByProviderId(int providerId) {
        var query = getDslContext().selectFrom(PROVIDER_CHAT)
                .where(PROVIDER_CHAT.PROVIDER_ID.eq(providerId)
                        .and(PROVIDER_CHAT.ENABLED));
        return Optional.ofNullable(fetchOne(query, providerChatRowMapper));
    }

    public void save(ProviderChat providerChat) {
        var record = getDslContext().newRecord(PROVIDER_CHAT, providerChat);
        var query = getDslContext().insertInto(PROVIDER_CHAT)
                .set(record);
        executeOne(query);
    }

    public void disable(Integer providerId) {
        //TODO: return counter for affected rows and warn user if nothing changed?
        var set = getDslContext().update(PROVIDER_CHAT)
                .set(PROVIDER_CHAT.ENABLED, false)
                .where(PROVIDER_CHAT.PROVIDER_ID.eq(providerId));
        execute(set);
    }
}
