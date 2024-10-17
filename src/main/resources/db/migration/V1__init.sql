CREATE
EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS dspt_tg_bot;

CREATE TYPE dspt_tg_bot.chat_type AS ENUM ('merchant', 'provider', 'support');

CREATE TABLE dspt_tg_bot.chat
(
    id          BIGSERIAL             NOT NULL,
    title       CHARACTER VARYING,
    type        dspt_tg_bot.chat_type NOT NULL,
    locale      CHARACTER VARYING,

    CONSTRAINT chat_id_pkey PRIMARY KEY (id));

CREATE TYPE dspt_tg_bot.dispute_status AS ENUM ('pending', 'approved', 'declined');

CREATE TABLE dspt_tg_bot.merchant_dispute
(
    id                BIGSERIAL                  NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    chat_id           BIGSERIAL                  NOT NULL,
    tg_message_id     BIGINT                     NOT NULL,
    tg_message_locale CHARACTER VARYING,
    invoice_id        CHARACTER VARYING          NOT NULL,
    payment_id        CHARACTER VARYING          NOT NULL,
    external_id       CHARACTER VARYING,
    dispute_id        CHARACTER VARYING          NOT NULL,
    status            dspt_tg_bot.dispute_status NOT NULL DEFAULT 'pending' :: dspt_tg_bot.dispute_status,
    message           CHARACTER VARYING,

    CONSTRAINT dispute_merchant_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES dspt_tg_bot.chat (id),
    UNIQUE (dispute_id)
);

CREATE TABLE dspt_tg_bot.provider_dispute
(
    id              UUID              NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    replied_at      TIMESTAMP WITHOUT TIME ZONE,
    chat_id         BIGSERIAL         NOT NULL,
    tg_message_id   BIGINT,
    invoice_id      CHARACTER VARYING NOT NULL,
    payment_id      CHARACTER VARYING NOT NULL,
    provider_trx_id CHARACTER VARYING,
    reason          CHARACTER VARYING,
    reply_text      CHARACTER VARYING,

    CONSTRAINT dispute_provider_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES dspt_tg_bot.chat (id)
);

CREATE TABLE dspt_tg_bot.support_dispute
(
    id                  BIGSERIAL NOT NULL,
    provider_dispute_id UUID      NOT NULL,
    tg_message_id       BIGINT    NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    replied_at          TIMESTAMP WITHOUT TIME ZONE,
    chat_id             BIGSERIAL NOT NULL,
    reply_text          CHARACTER VARYING,
    is_approved         BOOLEAN,

    CONSTRAINT dispute_support_id_pkey PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES dspt_tg_bot.chat (id),
    FOREIGN KEY (provider_dispute_id) REFERENCES dspt_tg_bot.provider_dispute (id),
    UNIQUE (tg_message_id, chat_id)
);