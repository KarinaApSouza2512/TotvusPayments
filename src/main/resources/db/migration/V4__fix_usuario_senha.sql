-- Corrige hash BCrypt do usuário admin (hash anterior era inválido para "admin123")
-- Senha: admin123
UPDATE usuarios
SET senha = '$2a$10$6Ds7Ytac5Pq8PYU/RKqaxO0EjmpcexRdiHq/qPrpYmbAn2BSSzmme'
WHERE email = 'admin@totvus.com';