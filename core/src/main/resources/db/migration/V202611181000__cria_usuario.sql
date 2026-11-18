-- Usuário que entra na interface web (RF06). Modelo de dados, bloco A, seção 2.3.
-- A senha nunca é gravada: só o hash gerado pelo Spring Security (RNF02). O prefixo do hash,
-- como {bcrypt}, diz qual algoritmo o gerou.
-- Pela collation (C3), "Ana" e "ana" são o mesmo login.
CREATE TABLE usuario (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    login           VARCHAR(50)  NOT NULL,
    senha_hash      VARCHAR(255) NOT NULL,
    destinatario_id BIGINT       NOT NULL,
    criado_em       DATETIME(3)  NOT NULL,
    criado_por      VARCHAR(50)  NOT NULL,
    atualizado_em   DATETIME(3)  NOT NULL,
    atualizado_por  VARCHAR(50)  NOT NULL,
    versao          BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT fk_usuario_destinatario FOREIGN KEY (destinatario_id) REFERENCES destinatario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
