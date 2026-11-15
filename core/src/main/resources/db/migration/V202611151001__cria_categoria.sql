-- Categoria de receita ou de despesa (RF02). Modelo de dados, bloco A, seção 2.5.
-- Categorias nunca são excluídas: são desativadas (coluna `ativa`).
CREATE TABLE categoria (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nome           VARCHAR(50)  NOT NULL,
    tipo           VARCHAR(10)  NOT NULL,
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    -- O mesmo nome pode existir como receita e como despesa (ex.: Investimentos, ERS Apêndice A).
    CONSTRAINT uk_categoria_nome_tipo UNIQUE (nome, tipo),
    CONSTRAINT ck_categoria_tipo CHECK (tipo IN ('RECEITA', 'DESPESA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
