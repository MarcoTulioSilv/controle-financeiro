-- Fatura de um cartão em um mês de referência (RF14, RF15). Modelo de dados, bloco B, seção 3.4.
-- O mês de referência é o do vencimento NOMINAL (RN03); o vencimento efetivo é o próximo dia
-- útil a partir dele (RF16) e serve só para os avisos de pagamento.
-- `total_informado_banco` fica nulo até a revisão da fatura (RF15, fora do MVP 1).
CREATE TABLE fatura (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    cartao_id               BIGINT        NOT NULL,
    mes_referencia          DATE          NOT NULL,
    data_fechamento         DATE          NOT NULL,
    data_vencimento         DATE          NOT NULL,
    data_vencimento_efetiva DATE          NOT NULL,
    status                  VARCHAR(15)   NOT NULL,
    total_informado_banco   DECIMAL(12,2) NULL,
    criado_em               DATETIME(3)   NOT NULL,
    criado_por              VARCHAR(50)   NOT NULL,
    atualizado_em           DATETIME(3)   NOT NULL,
    atualizado_por          VARCHAR(50)   NOT NULL,
    versao                  BIGINT        NOT NULL,
    PRIMARY KEY (id),
    -- Uma fatura por cartão por mês. Também decide a corrida entre duas compras simultâneas que
    -- tentam criar a mesma fatura: só uma grava; a outra reaproveita a fatura criada (ServicoFaturas).
    CONSTRAINT uk_fatura_cartao_mes UNIQUE (cartao_id, mes_referencia),
    CONSTRAINT fk_fatura_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT ck_fatura_mes_referencia_dia_1 CHECK (DAY(mes_referencia) = 1),
    CONSTRAINT ck_fatura_status CHECK (status IN ('ABERTA', 'EM_REVISAO', 'CONFERIDA', 'PAGA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
