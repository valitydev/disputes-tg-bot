package dev.vality.disputes.tg.bot.common.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.enums.ChatType;
import dev.vality.disputes.tg.bot.domain.tables.pojos.Chat;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.domain.tables.Chat.CHAT;

@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class ChatDao extends AbstractGenericDao {

    private final RowMapper<Chat> chatRowMapper;

    @Autowired
    public ChatDao(DataSource dataSource) {
        super(dataSource);
        chatRowMapper = new RecordRowMapper<>(CHAT, Chat.class);
    }

    public Optional<Chat> get(long id) {
        var query = getDslContext().selectFrom(CHAT)
                .where(CHAT.ID.eq(id));
        return Optional.ofNullable(fetchOne(query, chatRowMapper));
    }

    public Optional<Chat> getProviderChatById(long id) {
        var query = getDslContext().selectFrom(CHAT)
                .where(CHAT.ID.eq(id)
                        .and(CHAT.TYPE.eq(ChatType.provider)));
        return Optional.ofNullable(fetchOne(query, chatRowMapper));
    }

    public Chat getSupportChat() {
        var query = getDslContext().selectFrom(CHAT)
                .where(CHAT.TYPE.eq(ChatType.support));
        return Optional.ofNullable(fetchOne(query, chatRowMapper)).orElseThrow();
    }
}
