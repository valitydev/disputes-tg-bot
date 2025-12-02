ALTER TABLE dspt_tg_bot.provider_chat
ADD COLUMN terminal_id INTEGER;

DROP INDEX IF EXISTS dspt_tg_bot.enabled_provider_chat_constraint;

CREATE UNIQUE INDEX enabled_provider_chat_constraint ON dspt_tg_bot.provider_chat (provider_id, terminal_id)
    WHERE enabled;
