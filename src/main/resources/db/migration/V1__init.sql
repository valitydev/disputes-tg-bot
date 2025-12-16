CREATE
EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS dspt_tg_bot;

CREATE TABLE dspt_tg_bot.merchant_chat
(
    id          BIGSERIAL             NOT NULL,
    chat_id     BIGINT                NOT NULL,
    title       CHARACTER VARYING,
    locale      CHARACTER VARYING,
    enabled     BOOLEAN DEFAULT TRUE  NOT NULL,

    CONSTRAINT merchant_chat_id_pkey PRIMARY KEY (id),
    UNIQUE (chat_id)
);

CREATE TYPE dspt_tg_bot.dispute_status AS ENUM ('pending', 'approved', 'declined');

CREATE TABLE dspt_tg_bot.merchant_dispute
(
    id                UUID                        NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    chat_id           BIGINT                      NOT NULL,
    tg_message_id     BIGINT                      NOT NULL,
    invoice_id        CHARACTER VARYING           NOT NULL,
    payment_id        CHARACTER VARYING           NOT NULL,
    external_id       CHARACTER VARYING,
    status            dspt_tg_bot.dispute_status  NOT NULL DEFAULT 'pending' :: dspt_tg_bot.dispute_status,
    message           CHARACTER VARYING,
    changed_amount    BIGINT,

    CONSTRAINT dispute_merchant_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES dspt_tg_bot.merchant_chat (id)
);

CREATE TABLE dspt_tg_bot.provider_chat
(
    id                  BIGSERIAL             NOT NULL,
    provider_id         INTEGER               NOT NULL,
    send_to_chat_id     BIGINT                NOT NULL,
    send_to_topic_id    INTEGER,
    read_from_chat_id   BIGINT                NOT NULL,
    template            CHARACTER VARYING,
    enabled             BOOLEAN DEFAULT TRUE  NOT NULL,

    CONSTRAINT provider_chat_id_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX enabled_provider_chat_constraint ON dspt_tg_bot.provider_chat (provider_id)
    WHERE enabled;

CREATE TABLE dspt_tg_bot.provider_dispute
(
    id              UUID              NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    forwarded_at    TIMESTAMP WITHOUT TIME ZONE,
    chat_id         BIGINT,
    tg_message_id   BIGINT,
    invoice_id      CHARACTER VARYING NOT NULL,
    payment_id      CHARACTER VARYING NOT NULL,
    provider_trx_id CHARACTER VARYING,
    reason          CHARACTER VARYING,

    CONSTRAINT dispute_provider_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES dspt_tg_bot.provider_chat (id)
);

CREATE TABLE dspt_tg_bot.provider_reply
(
    id              BIGSERIAL         NOT NULL,
    dispute_id      UUID              NOT NULL,
    replied_at      TIMESTAMP WITHOUT TIME ZONE,
    username        CHARACTER VARYING NOT NULL,
    reply_text      CHARACTER VARYING,

    CONSTRAINT provider_reply_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (dispute_id) REFERENCES dspt_tg_bot.provider_dispute (id)
);

CREATE TABLE dspt_tg_bot.support_dispute_review
(
    id                  BIGSERIAL NOT NULL,
    provider_dispute_id UUID      NOT NULL,
    tg_message_id       BIGINT    NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    replied_at          TIMESTAMP WITHOUT TIME ZONE,
    reply_text          CHARACTER VARYING,
    is_approved         BOOLEAN,

    CONSTRAINT dispute_support_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (provider_dispute_id) REFERENCES dspt_tg_bot.provider_dispute (id)
);