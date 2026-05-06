-- JWT 미구현 동안 userId=1L 하드코딩 유지를 위한 시드
INSERT INTO users (user_id, nickname, email, is_active, created_at, updated_at)
VALUES (1, '테스트유저', 'test@test.com', true, now(), now())
ON CONFLICT (user_id) DO NOTHING;

SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
