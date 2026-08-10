BEGIN;

DELETE FROM dspt_tg_bot.merchant_party
WHERE id = 13
  AND merchant_chat_id = 18
  AND party_id = 'a318bfeb-ec5b-44fe-82e6-8e1455e6250a';

COMMIT;
