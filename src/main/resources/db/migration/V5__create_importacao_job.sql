CREATE TABLE importacao_jobs (
    protocolo       VARCHAR(36)  PRIMARY KEY,
    status          VARCHAR(30)  NOT NULL,
    total_linhas    INTEGER      NOT NULL DEFAULT 0,
    total_sucesso   INTEGER      NOT NULL DEFAULT 0,
    total_erros     INTEGER      NOT NULL DEFAULT 0,
    criado_em       TIMESTAMP    NOT NULL,
    atualizado_em   TIMESTAMP    NOT NULL,
    CONSTRAINT chk_importacao_status CHECK (status IN ('PROCESSANDO', 'CONCLUIDO', 'CONCLUIDO_COM_ERROS', 'FALHA'))
);
