package dev.vality.disputes.tg.bot.core.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.disputes.tg.bot.core.domain.tables.pojos.MerchantParty;
import dev.vality.mapper.RecordRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static dev.vality.disputes.tg.bot.core.domain.Tables.MERCHANT_PARTY;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantPartyDao extends AbstractGenericDao {

    private final RowMapper<MerchantParty> merchantPartyRowMapper;

    @Autowired
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
} 