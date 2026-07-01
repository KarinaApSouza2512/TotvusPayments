CREATE TABLE usuarios (
    id    BIGSERIAL    PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role  VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER'
);

-- Usuário padrão: admin@totvus.com / admin123
INSERT INTO usuarios (email, senha, role)
VALUES (
    'admin@totvus.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh32',
    'ROLE_ADMIN'
);