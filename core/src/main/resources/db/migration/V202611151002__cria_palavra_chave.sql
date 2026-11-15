-- Palavra-chave que leva o bot a deduzir a categoria (RF02). Modelo de dados, bloco A, seção 2.6.
-- O UNIQUE em `texto` faz cada palavra pertencer a uma única categoria. Pela collation
-- utf8mb4_0900_ai_ci (C3), "farmacia" e "Farmácia" são a mesma palavra-chave.
CREATE TABLE palavra_chave (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    texto          VARCHAR(50)  NOT NULL,
    categoria_id   BIGINT       NOT NULL,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_palavra_chave_texto UNIQUE (texto),
    CONSTRAINT fk_palavra_chave_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
