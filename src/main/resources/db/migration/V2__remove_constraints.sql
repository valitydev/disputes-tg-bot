ALTER TABLE dspt_tg_bot.support_dispute_review ALTER COLUMN provider_dispute_id DROP NOT NULL;
ALTER TABLE dspt_tg_bot.support_dispute_review ADD COLUMN invoice_id CHARACTER VARYING;
ALTER TABLE dspt_tg_bot.support_dispute_review ADD COLUMN payment_id CHARACTER VARYING;
