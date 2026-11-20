-- Dia sem expediente bancário, usado no vencimento efetivo das faturas (RF16).
-- Modelo de dados, bloco B, seção 3.6. Os nacionais são gravados pela aplicação a partir do
-- CalendarioBancario; os locais (ex.: municipais) vêm do arquivo de dados iniciais.
CREATE TABLE feriado (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    data           DATE         NOT NULL,
    descricao      VARCHAR(100) NOT NULL,
    abrangencia    VARCHAR(10)  NOT NULL,
    criado_em      DATETIME(3)  NOT NULL,
    criado_por     VARCHAR(50)  NOT NULL,
    atualizado_em  DATETIME(3)  NOT NULL,
    atualizado_por VARCHAR(50)  NOT NULL,
    versao         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_feriado_data UNIQUE (data),
    CONSTRAINT ck_feriado_abrangencia CHECK (abrangencia IN ('NACIONAL', 'LOCAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
