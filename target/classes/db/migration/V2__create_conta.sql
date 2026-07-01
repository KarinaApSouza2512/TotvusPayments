CREATE TABLE contas (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id    BIGINT       NOT NULL REFERENCES fornecedores(id),
    data_vencimento  DATE         NOT NULL,
    data_pagamento   DATE,
    valor            NUMERIC(15, 2) NOT NULL,
    descricao        VARCHAR(500) NOT NULL,
    situacao         VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT chk_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_situacao CHECK (situacao IN ('PENDENTE', 'PAGO', 'CANCELADO'))
);

CREATE INDEX idx_contas_data_vencimento ON contas (data_vencimento);
CREATE INDEX idx_contas_situacao        ON contas (situacao);
CREATE INDEX idx_contas_fornecedor_id   ON contas (fornecedor_id);