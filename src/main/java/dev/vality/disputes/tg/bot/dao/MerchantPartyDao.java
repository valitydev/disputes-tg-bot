package dev.vality.disputes.tg.bot.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.domain.tables.pojos.MerchantParty;
import dev.vality.disputes.tg.bot.exception.NotFoundException;
import dev.vality.mapper.RecordRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.domain.Tables.MERCHANT_PARTY;

@Slf4j
@Component
public class MerchantPartyDao extends AbstractGenericDao {

    private final RowMapper<MerchantParty> merchantPartyRowMapper;

    public MerchantPartyDao(DataSource dataSource) {
        super(dataSource);
        merchantPartyRowMapper = new RecordRowMapper<>(MERCHANT_PARTY, MerchantParty.class);
    }

    public Optional<MerchantParty> getByPartyId(String partyId) {
        var query = getDslContext().selectFrom(MERCHANT_PARTY)
                .where(MERCHANT_PARTY.PARTY_ID.eq(partyId));
        return Optional.ofNullable(fetchOne(query, merchantPartyRowMapper));
    }

    public Optional<MerchantParty> getByMerchantChatId(Long merchantChatId) {
        var query = getDslContext().selectFrom(MERCHANT_PARTY)
                .where(MERCHANT_PARTY.MERCHANT_CHAT_ID.eq(merchantChatId));
        return Optional.ofNullable(fetchOne(query, merchantPartyRowMapper));
    }

    public List<MerchantParty> getAllByMerchantChatId(Long merchantChatId) {
        var query = getDslContext().selectFrom(MERCHANT_PARTY)
                .where(MERCHANT_PARTY.MERCHANT_CHAT_ID.eq(merchantChatId));
        return fetch(query, merchantPartyRowMapper);
    }

    public void save(MerchantParty merchantParty) {
        var record = getDslContext().newRecord(MERCHANT_PARTY, merchantParty);
        var query = getDslContext().insertInto(MERCHANT_PARTY)
                .set(record);
        executeOne(query);
    }

    public int deleteAllParties(Long merchantChatId) {
        var query = getDslContext().deleteFrom(MERCHANT_PARTY)
                .where(MERCHANT_PARTY.MERCHANT_CHAT_ID.eq(merchantChatId));
        return execute(query);
    }

    public void deleteParty(Long merchantChatId, String partyId) {
        var query = getDslContext().deleteFrom(MERCHANT_PARTY)
                .where(MERCHANT_PARTY.MERCHANT_CHAT_ID.eq(merchantChatId).and(MERCHANT_PARTY.PARTY_ID.eq(partyId)));
        try {
            executeOne(query);
        } catch (JdbcUpdateAffectedIncorrectNumberOfRowsException e) {
            throw new NotFoundException("party_id not found");
        }
    }
}