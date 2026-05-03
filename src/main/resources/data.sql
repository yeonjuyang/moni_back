INSERT INTO users (user_id, nickname, email, is_active, created_at, updated_at)
VALUES (1, '테스트유저', 'test@test.com', true, now(), now())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO ledger (ledger_id, ledger_name, ledger_type, created_by, is_active, created_at, updated_at)
VALUES (1, '테스트 가계부', 'PERSONAL', 1, true, now(), now())
ON CONFLICT (ledger_id) DO NOTHING;

INSERT INTO category (category_id, ledger_id, category_name, category_type, sort_order, is_active, created_at, updated_at)
VALUES
  (1, 1, '급여', 'INCOME', 1, true, now(), now()),
  (2, 1, '식비', 'EXPENSE', 1, true, now(), now()),
  (3, 1, '카페', 'EXPENSE', 2, true, now(), now()),
  (4, 1, '교통', 'EXPENSE', 3, true, now(), now())
ON CONFLICT (category_id) DO NOTHING;

INSERT INTO asset (asset_id, ledger_id, asset_name, asset_type, balance, sort_order, is_active, created_at, updated_at)
VALUES (1, 1, '내 통장', 'BANK', 0, 1, true, now(), now())
ON CONFLICT (asset_id) DO NOTHING;

-- INCOME: from_asset_id=NULL, to_asset_id=NOT NULL, category_id=NOT NULL
-- EXPENSE: from_asset_id=NOT NULL, to_asset_id=NULL, category_id=NOT NULL
INSERT INTO transaction_record (transaction_id, ledger_id, transaction_type, category_id, amount, memo, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
  (1, 1, 'INCOME', 1, 3000000, '월급',       '2026-04-01', NULL, 1,    1, 1, now(), now()),
  (2, 1, 'EXPENSE', 2, 12000,  '점심 식사',   '2026-04-08', 1,    NULL, 1, 1, now(), now()),
  (3, 1, 'EXPENSE', 3,  6500,  '스타벅스',    '2026-04-03', 1,    NULL, 1, 1, now(), now()),
  (4, 1, 'EXPENSE', 4, 50000,  '교통카드 충전','2026-04-12', 1,    NULL, 1, 1, now(), now()),
  (5, 1, 'EXPENSE', 2, 35000,  '저녁 외식',   '2026-04-18', 1,    NULL, 1, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('ledger_ledger_id_seq', (SELECT MAX(ledger_id) FROM ledger));
SELECT setval('category_category_id_seq', (SELECT MAX(category_id) FROM category));
SELECT setval('asset_asset_id_seq', (SELECT MAX(asset_id) FROM asset));
SELECT setval('transaction_record_transaction_id_seq', (SELECT MAX(transaction_id) FROM transaction_record));
