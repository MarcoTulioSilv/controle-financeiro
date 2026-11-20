-- Cartão de crédito (RF14). Modelo de dados, bloco B, seção 3.3.
-- O apelido é a palavra que identifica o cartão no bot (ex.: `bmt`). Ele também não pode
-- coincidir com uma palavra-chave nem com a marcação de um destinatário, mas essa regra fica na
-- aplicação (ServicoCartoes): são tabelas diferentes, e o UNIQUE só atua dentro de uma tabela.
-- Cartão cancelado é desativado (`ativo`), nunca excluído: as faturas antigas apontam para ele.
CREATE TABLE cartao (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    apelido              VARCHAR(20)  NOT NULL,
    nome                 VARCHAR(50)  NOT NULL,
    titular_id           BIGINT       NOT NULL,
    dia_fechamento       TINYINT      NOT NULL,
    dia_vencimento       TINYINT      NOT NULL,
    compra_no_fechamento VARCHAR(10)  NOT NULL,
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em            DATETIME(3)  NOT NULL,
    criado_por           VARCHAR(50)  NOT NULL,
    atualizado_em        DATETIME(3)  NOT NULL,
    atualizado_por       VARCHAR(50)  NOT NULL,
    versao               BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cartao_apelido UNIQUE (apelido),
    CONSTRAINT fk_cartao_titular FOREIGN KEY (titular_id) REFERENCES usuario (id),
    -- O dia é o do mês do cartão; meses mais curtos usam o último dia (RF14), no cálculo.
    CONSTRAINT ck_cartao_dia_fechamento CHECK (dia_fechamento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_dia_vencimento CHECK (dia_vencimento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_compra_no_fechamento CHECK (compra_no_fechamento IN ('ATUAL', 'PROXIMA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
