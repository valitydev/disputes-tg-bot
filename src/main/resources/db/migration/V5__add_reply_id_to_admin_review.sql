ALTER TABLE dspt_tg_bot.admin_dispute_review
ADD COLUMN provider_reply_id BIGINT;
ALTER TABLE dspt_tg_bot.admin_dispute_review
ADD CONSTRAINT admin_dispute_review_provider_reply_id_fk FOREIGN KEY (provider_reply_id) REFERENCES dspt_tg_bot.provider_reply (id);
