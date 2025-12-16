CREATE TABLE dspt_tg_bot.merchant_party
(
    id          BIGSERIAL             NOT NULL,
    merchant_chat_id BIGINT           NOT NULL,
    party_id    CHARACTER VARYING     NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT merchant_party_id_pkey PRIMARY KEY (id),
    CONSTRAINT merchant_party_merchant_chat_fk FOREIGN KEY (merchant_chat_id) REFERENCES dspt_tg_bot.merchant_chat (id),
    CONSTRAINT merchant_party_party_id_unique UNIQUE (party_id)
);

CREATE INDEX idx_merchant_party_party_id ON dspt_tg_bot.merchant_party (party_id);
CREATE INDEX idx_merchant_party_merchant_chat_id ON dspt_tg_bot.merchant_party (merchant_chat_id);

ALTER TABLE dspt_tg_bot.support_dispute_review RENAME TO admin_dispute_review;
ALTER TABLE dspt_tg_bot.admin_dispute_review RENAME CONSTRAINT dispute_support_id_pkey TO dispute_admin_id_pkey;