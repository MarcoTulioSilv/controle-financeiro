-- Meta de economia: percentual da receita do mês que o domicílio quer economizar (RF04).
-- Modelo de dados, bloco D, seção 5.4. Mesma vigência do orçamento: vale a linha mais recente
-- com `vigente_desde` menor ou igual ao mês.
CREATE TABLE meta_economia (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    vigente_desde  DATE         NOT NULL,
    percentual     DECIMAL(5,2) NOT NULL,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meta_economia_vigencia UNIQUE (vigente_desde),
    CONSTRAINT ck_meta_economia_vigente_desde_dia_1 CHECK (DAY(vigente_desde) = 1),
    CONSTRAINT ck_meta_economia_percentual CHECK (percentual BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
