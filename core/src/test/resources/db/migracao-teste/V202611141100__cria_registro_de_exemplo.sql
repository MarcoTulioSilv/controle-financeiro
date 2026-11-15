-- Tabela de apoio dos testes do pacote `compartilhado` (não existe na aplicação).
-- Mostra as colunas padrão da convenção C7 e o mês de referência da C5.
CREATE TABLE registro_de_exemplo (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    descricao      VARCHAR(50)  NOT NULL,
    mes_referencia DATE         NOT NULL,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_registro_de_exemplo_mes_referencia CHECK (DAY(mes_referencia) = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
