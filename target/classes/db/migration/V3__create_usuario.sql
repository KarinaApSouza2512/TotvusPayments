CREATE TABLE usuarios (
    id    BIGSERIAL    PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role  VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER'
);

-- Usuário padrão: admin@totvus.com / admin123
-- Hash gerado com BCrypt rounds=10
INSERT INTO usuarios (email, senha, role)
VALUES (
    'admin@totvus.com',
    '$2a$10$1G8bKVjzBHlQeRy5aBJQceg0x2W8RiPqVdZLx9wPu8CgYi4U/pqMG',
    'ROLE_ADMIN'
);