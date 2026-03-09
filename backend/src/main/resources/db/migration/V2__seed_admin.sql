INSERT INTO users (username, email, password_hash, role, is_enabled, created_at, updated_at)
VALUES (
    'admin',
    'admin@test.com',
    '$2a$10$JSG8IEW5ktcqaG7ePkdch.qCgmuTJIdporpUJVWiYhnIVYpu8NKJy',
    'ADMIN',
    true,
    NOW(),
    NOW()
);