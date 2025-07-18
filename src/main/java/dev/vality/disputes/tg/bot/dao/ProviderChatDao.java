package dev.vality.disputes.tg.bot.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.ProviderChat;
import dev.vality.mapper.RecordRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.domain.Tables.PROVIDER_CHAT;

@Slf4j
@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class ProviderChatDao extends AbstractGenericDao {

    private final RowMapper<ProviderChat> providerChatRowMapper;

    public ProviderChatDao(DataSource dataSource) {
        super(dataSource);
        providerChatRowMapper = new RecordRowMapper<>(PROVIDER_CHAT, ProviderChat.class);
    }

    public Optional<ProviderChat> get(long id) {
        var query = getDslContext().selectFrom(PROVIDER_CHAT)
                .where(PROVIDER_CHAT.READ_FROM_CHAT_ID.eq(id).and(PROVIDER_CHAT.ENABLED));
        return Optional.ofNullable(fetchOne(query, providerChatRowMapper));
    }

    public boolean exists(long id) {
        return getDslContext().fetchCount(PROVIDER_CHAT,
                PROVIDER_CHAT.READ_FROM_CHAT_ID.eq(id).and(PROVIDER_CHAT.ENABLED)) > 0;
    }

    public Optional<ProviderChat> getByProviderId(int providerId) {
        var query = getDslContext().selectFrom(PROVIDER_CHAT)
                .where(PROVIDER_CHAT.PROVIDER_ID.eq(providerId)
                        .and(PROVIDER_CHAT.ENABLED));
        return Optional.ofNullable(fetchOne(query, providerChatRowMapper));
    }

    public List<ProviderChat> getByReadFromChatId(long chatId) {
        var query = getDslContext().selectFrom(PROVIDER_CHAT)
                .where(PROVIDER_CHAT.READ_FROM_CHAT_ID.eq(chatId)
                        .and(PROVIDER_CHAT.ENABLED));
        return fetch(query, providerChatRowMapper);
    }

    public void save(ProviderChat providerChat) {
        var record = getDslContext().newRecord(PROVIDER_CHAT, providerChat);
        var query = getDslContext().insertInto(PROVIDER_CHAT)
                .set(record);
        executeOne(query);
    }

    public void disable(Integer providerId) {
        try {
            var set = getDslContext().update(PROVIDER_CHAT)
                    .set(PROVIDER_CHAT.ENABLED, false)
                    .where(PROVIDER_CHAT.PROVIDER_ID.eq(providerId).and(PROVIDER_CHAT.ENABLED));
            executeOne(set);
        } catch (JdbcUpdateAffectedIncorrectNumberOfRowsException e1) {
            log.warn("Unable to disable provider chat. Expected 1 row, got {}", e1.getActualRowsAffected());
        }
    }
} 