-- Destinatário: a quem um lançamento se destina, o "para quem" do RF13.
-- Modelo de dados, bloco A, seção 2.2. É uma tabela (e não um enum) para que um novo membro da
-- família seja apenas uma linha nova.
CREATE TABLE destinatario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nome           VARCHAR(50)  NOT NULL,
    marcacao_bot   VARCHAR(20)  NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_destinatario_nome UNIQUE (nome),
    CONSTRAINT uk_destinatario_marcacao_bot UNIQUE (marcacao_bot)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
