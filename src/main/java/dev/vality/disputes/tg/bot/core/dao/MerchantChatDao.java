package dev.vality.disputes.tg.bot.core.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantChat;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.domain.Tables.MERCHANT_CHAT;

@Component
@SuppressWarnings({"ParameterName", "LineLength"})
public class MerchantChatDao extends AbstractGenericDao {

    private final RowMapper<MerchantChat> merchantChatRowMapper;

    @Autowired
    public MerchantChatDao(DataSource dataSource) {
        super(dataSource);
        merchantChatRowMapper = new RecordRowMapper<>(MERCHANT_CHAT, MerchantChat.class);
    }

    public Optional<MerchantChat> get(long tgChatId) {
        var query = getDslContext().selectFrom(MERCHANT_CHAT)
                .where(MERCHANT_CHAT.CHAT_ID.eq(tgChatId));
        return Optional.ofNullable(fetchOne(query, merchantChatRowMapper));
    }

    public Optional<MerchantChat> getById(long id) {
        var query = getDslContext().selectFrom(MERCHANT_CHAT)
                .where(MERCHANT_CHAT.ID.eq(id));
        return Optional.ofNullable(fetchOne(query, merchantChatRowMapper));
    }

    public void save(MerchantChat merchantChat) {
        //TODO: return id? catch duplicate exception?
        var record = getDslContext().newRecord(MERCHANT_CHAT, merchantChat);
        var query = getDslContext().insertInto(MERCHANT_CHAT)
                .set(record);
        executeOne(query);
    }

    public void disable(Long chatId) {
        //TODO: return counter for affected rows and warn user if nothing changed?
        var set = getDslContext().update(MERCHANT_CHAT)
                .set(MERCHANT_CHAT.ENABLED, false)
                .where(MERCHANT_CHAT.CHAT_ID.eq(chatId));
        executeOne(set);
    }
} 