-- ================================================================
-- Users
-- ================================================================
INSERT INTO users (user_id, nickname, email, is_active, created_at, updated_at)
VALUES
    (1, '양씨',   'test@test.com',    true, now(), now()),
    (2, '박민준', 'partner@test.com', true, now(), now())
ON CONFLICT DO NOTHING;

-- ================================================================
-- Ledger
-- ================================================================
INSERT INTO ledger (ledger_id, ledger_name, ledger_type, created_by, is_active, created_at, updated_at)
VALUES (1, '우리 가계부', 'SHARED', 1, true, now(), now())
ON CONFLICT DO NOTHING;

-- ================================================================
-- Ledger Members
-- ================================================================
INSERT INTO ledger_member (id, ledger_id, user_id, role, nickname, joined_at)
VALUES
    (1, 1, 1, 'OWNER',  '양씨', now()),
    (2, 1, 2, 'MEMBER', '민준', now())
ON CONFLICT DO NOTHING;

-- ================================================================
-- Categories
-- ================================================================
INSERT INTO category (category_id, ledger_id, category_name, category_type, icon_name, icon_color, sort_order, is_active, created_at, updated_at)
VALUES
    (1,  1, '식비', 'EXPENSE', 'restaurant', '#FF6B6B', 1, true, now(), now()),
    (2,  1, '카페', 'EXPENSE', 'coffee',     '#FFB347', 2, true, now(), now()),
    (3,  1, '교통', 'EXPENSE', 'bus',        '#6BCB77', 3, true, now(), now()),
    (4,  1, '쇼핑', 'EXPENSE', 'shopping',   '#4D96FF', 4, true, now(), now()),
    (5,  1, '의료', 'EXPENSE', 'hospital',   '#FF6BAA', 5, true, now(), now()),
    (6,  1, '주거', 'EXPENSE', 'home',       '#4DC9D6', 6, true, now(), now()),
    (7,  1, '기타', 'EXPENSE', 'other',      '#9E9E9E', 7, true, now(), now()),
    (8,  1, '급여', 'INCOME',  'salary',     '#26A69A', 1, true, now(), now()),
    (9,  1, '부업', 'INCOME',  'work',       '#42A5F5', 2, true, now(), now()),
    (10, 1, '기타', 'INCOME',  'other',      '#9E9E9E', 3, true, now(), now())
ON CONFLICT DO NOTHING;

-- ================================================================
-- Assets
-- ================================================================
INSERT INTO asset (asset_id, ledger_id, asset_name, asset_type, balance, sort_order, is_active, created_at, updated_at)
VALUES
    (1, 1, '주거래 통장', 'BANK', 2500000, 1, true, now(), now()),
    (2, 1, '현금',       'CASH', 150000,  2, true, now(), now()),
    (3, 1, '신용카드',   'CARD', 0,       3, true, now(), now())
ON CONFLICT (asset_id) DO NOTHING;

-- ================================================================
-- Transactions (1~6월)
-- transaction_type: EXPENSE → from_asset_id 필수 / INCOME → to_asset_id 필수
-- ================================================================

-- 1월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (1,  1, 'INCOME',  8,  3500000, '1월 급여',     null, '2026-01-05', null, 1, 1, 1, now(), now()),
    (2,  1, 'EXPENSE', 6,   600000, '월세',          null, '2026-01-10', 1, null, 1, 1, now(), now()),
    (3,  1, 'EXPENSE', 3,    50000, '교통카드 충전', null, '2026-01-11', 1, null, 1, 1, now(), now()),
    (4,  1, 'EXPENSE', 1,    45000, '마트 장보기',   null, '2026-01-12', 1, null, 1, 1, now(), now()),
    (5,  1, 'EXPENSE', 1,    12500, '점심 식사',     null, '2026-01-15', 2, null, 1, 1, now(), now()),
    (6,  1, 'EXPENSE', 2,     4500, '아메리카노',    null, '2026-01-17', 2, null, 1, 1, now(), now()),
    (7,  1, 'EXPENSE', 4,   129000, '겨울 아우터',   null, '2026-01-18', 3, null, 2, 1, now(), now()),
    (8,  1, 'EXPENSE', 1,    38000, '저녁 외식',     null, '2026-01-20', 1, null, 2, 1, now(), now()),
    (9,  1, 'EXPENSE', 2,     6000, '카페라떼',      null, '2026-01-22', 2, null, 2, 1, now(), now()),
    (10, 1, 'INCOME',  9,   150000, '프리랜서 작업', null, '2026-01-28', null, 1, 2, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- 2월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (11, 1, 'INCOME',  8,  3500000, '2월 급여',   null, '2026-02-05', null, 1, 1, 1, now(), now()),
    (12, 1, 'EXPENSE', 6,   600000, '월세',        null, '2026-02-10', 1, null, 1, 1, now(), now()),
    (13, 1, 'EXPENSE', 1,    67000, '마트',         null, '2026-02-11', 1, null, 1, 1, now(), now()),
    (14, 1, 'EXPENSE', 1,    25000, '중식당',       null, '2026-02-14', 2, null, 1, 1, now(), now()),
    (15, 1, 'EXPENSE', 2,     5500, '라떼',         null, '2026-02-14', 2, null, 1, 1, now(), now()),
    (16, 1, 'EXPENSE', 5,    28000, '병원',         null, '2026-02-17', 1, null, 1, 1, now(), now()),
    (17, 1, 'EXPENSE', 3,    15000, '택시',         null, '2026-02-19', 2, null, 2, 1, now(), now()),
    (18, 1, 'EXPENSE', 1,    55000, '외식',         null, '2026-02-21', 1, null, 2, 1, now(), now()),
    (19, 1, 'EXPENSE', 4,    45000, '생활용품',     null, '2026-02-23', 3, null, 2, 1, now(), now()),
    (20, 1, 'INCOME',  9,   200000, '온라인 강의',  null, '2026-02-28', null, 1, 1, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- 3월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (21, 1, 'INCOME',  8,  3500000, '3월 급여',      null, '2026-03-05', null, 1, 1, 1, now(), now()),
    (22, 1, 'EXPENSE', 6,   600000, '월세',            null, '2026-03-10', 1, null, 1, 1, now(), now()),
    (23, 1, 'EXPENSE', 3,    50000, '교통카드',        null, '2026-03-11', 1, null, 1, 1, now(), now()),
    (24, 1, 'EXPENSE', 1,    78000, '마트',             null, '2026-03-13', 1, null, 1, 1, now(), now()),
    (25, 1, 'EXPENSE', 2,    12000, '카페 3회',         null, '2026-03-17', 2, null, 1, 1, now(), now()),
    (26, 1, 'EXPENSE', 4,    89000, '의류',             null, '2026-03-19', 3, null, 1, 1, now(), now()),
    (27, 1, 'EXPENSE', 1,    48000, '외식',             null, '2026-03-21', 1, null, 2, 1, now(), now()),
    (28, 1, 'EXPENSE', 2,     8500, '아메리카노 2회',   null, '2026-03-24', 2, null, 2, 1, now(), now()),
    (29, 1, 'EXPENSE', 5,    15000, '약국',             null, '2026-03-25', 2, null, 2, 1, now(), now()),
    (30, 1, 'INCOME',  9,   300000, '디자인 작업',      null, '2026-03-28', null, 1, 2, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- 4월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (31, 1, 'INCOME',  8,  3800000, '4월 급여(성과급)', null, '2026-04-05', null, 1, 1, 1, now(), now()),
    (32, 1, 'INCOME',  8,  2500000, '4월 급여',         null, '2026-04-05', null, 1, 2, 1, now(), now()),
    (33, 1, 'EXPENSE', 6,   600000, '월세',              null, '2026-04-10', 1, null, 1, 1, now(), now()),
    (34, 1, 'EXPENSE', 1,    52000, '마트',               null, '2026-04-12', 1, null, 1, 1, now(), now()),
    (35, 1, 'EXPENSE', 2,     9000, '카페',               null, '2026-04-14', 2, null, 1, 1, now(), now()),
    (36, 1, 'EXPENSE', 4,   215000, '봄 옷',              null, '2026-04-16', 3, null, 1, 1, now(), now()),
    (37, 1, 'EXPENSE', 3,    35000, '기차',               null, '2026-04-19', 1, null, 2, 1, now(), now()),
    (38, 1, 'EXPENSE', 1,    62000, '외식',               null, '2026-04-21', 1, null, 2, 1, now(), now()),
    (39, 1, 'EXPENSE', 2,     4500, '아메리카노',         null, '2026-04-23', 2, null, 2, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- 5월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (40, 1, 'INCOME',  8,  3500000, '5월 급여',   null, '2026-05-05', null, 1, 1, 1, now(), now()),
    (41, 1, 'EXPENSE', 6,   600000, '월세',        null, '2026-05-10', 1, null, 1, 1, now(), now()),
    (42, 1, 'EXPENSE', 3,    50000, '교통카드',    null, '2026-05-11', 1, null, 1, 1, now(), now()),
    (43, 1, 'EXPENSE', 1,    85000, '마트',         null, '2026-05-13', 1, null, 1, 1, now(), now()),
    (44, 1, 'EXPENSE', 2,    18000, '카페 4회',     null, '2026-05-16', 2, null, 1, 1, now(), now()),
    (45, 1, 'EXPENSE', 4,    78000, '여름 준비',    null, '2026-05-19', 3, null, 1, 1, now(), now()),
    (46, 1, 'EXPENSE', 1,    72000, '외식',         null, '2026-05-21', 1, null, 2, 1, now(), now()),
    (47, 1, 'EXPENSE', 2,    12500, '카페',         null, '2026-05-23', 2, null, 2, 1, now(), now()),
    (48, 1, 'INCOME',  9,   450000, '컨설팅',       null, '2026-05-28', null, 1, 1, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- 6월
INSERT INTO transaction_record
    (transaction_id, ledger_id, transaction_type, category_id, amount, memo, note, transaction_date, from_asset_id, to_asset_id, paid_by_user_id, created_by_user_id, created_at, updated_at)
VALUES
    (49, 1, 'INCOME',  8,  3500000, '6월 급여',   null, '2026-06-05', null, 1, 1, 1, now(), now()),
    (50, 1, 'INCOME',  8,  2500000, '6월 급여',   null, '2026-06-05', null, 1, 2, 1, now(), now()),
    (51, 1, 'EXPENSE', 6,   600000, '월세',        null, '2026-06-10', 1, null, 1, 1, now(), now()),
    (52, 1, 'EXPENSE', 1,    55000, '마트',         null, '2026-06-11', 1, null, 1, 1, now(), now()),
    (53, 1, 'EXPENSE', 2,     7500, '카페',         null, '2026-06-13', 2, null, 1, 1, now(), now()),
    (54, 1, 'EXPENSE', 3,    25000, '택시',         null, '2026-06-16', 2, null, 1, 1, now(), now()),
    (55, 1, 'EXPENSE', 4,    45000, '선물',         null, '2026-06-19', 3, null, 2, 1, now(), now()),
    (56, 1, 'EXPENSE', 1,    38000, '외식',         null, '2026-06-21', 1, null, 2, 1, now(), now())
ON CONFLICT (transaction_id) DO NOTHING;

-- ================================================================
-- Sequence 보정 (명시적 ID INSERT 후 필수)
-- ================================================================
SELECT setval('users_user_id_seq',                         (SELECT MAX(user_id)        FROM users));
SELECT setval('ledger_ledger_id_seq',                      (SELECT MAX(ledger_id)      FROM ledger));
SELECT setval('ledger_member_id_seq',                      (SELECT MAX(id)             FROM ledger_member));
SELECT setval('category_category_id_seq',                  (SELECT MAX(category_id)    FROM category));
SELECT setval('asset_asset_id_seq',                        (SELECT MAX(asset_id)       FROM asset));
SELECT setval('transaction_record_transaction_id_seq',     (SELECT MAX(transaction_id) FROM transaction_record));
