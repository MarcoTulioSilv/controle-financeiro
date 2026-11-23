-- Dívida: compra parcelada no cartão, financiamento ou empréstimo (RF12). Modelo de dados,
-- bloco B, seção 3.5 (ERS v1.4, sem a antiga coluna `lancamento_quitacao_id`).
--
-- Compra parcelada e dívida são a mesma tabela, distinguidas pelo `tipo` (seção 3.1): as duas
-- geram parcelas mensais na categoria Dívidas; a diferença é a presença do cartão.
--
-- O saldo devedor NÃO é uma coluna: é calculado a partir das parcelas (normalização).
--
-- `bem_id` (FK → bem, opcional) fica para o RF05: a tabela `bem` só nasce com o patrimônio,
-- depois do MVP 1, e a coluna entra junto com a chave estrangeira.
-- `data_quitacao` só é preenchida na quitação, que não acontece no MVP 1 (só o estado ATIVA).
CREATE TABLE divida (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    tipo                VARCHAR(20)   NOT NULL,
    descricao           VARCHAR(200)  NOT NULL,
    valor_total         DECIMAL(12,2) NOT NULL,
    quantidade_parcelas SMALLINT      NOT NULL,
    cartao_id           BIGINT        NULL,
    destinatario_id     BIGINT        NOT NULL,
    data_contratacao    DATE          NOT NULL,
    status              VARCHAR(10)   NOT NULL,
    data_quitacao       DATE          NULL,
    criado_em           DATETIME(3)   NOT NULL,
    criado_por          VARCHAR(50)   NOT NULL,
    atualizado_em       DATETIME(3)   NOT NULL,
    atualizado_por      VARCHAR(50)   NOT NULL,
    versao              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_divida_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT fk_divida_destinatario FOREIGN KEY (destinatario_id) REFERENCES destinatario (id),
    CONSTRAINT ck_divida_tipo CHECK (tipo IN ('PARCELAMENTO_CARTAO', 'FINANCIAMENTO', 'EMPRESTIMO')),
    CONSTRAINT ck_divida_valor_total_positivo CHECK (valor_total > 0),
    CONSTRAINT ck_divida_quantidade_parcelas CHECK (quantidade_parcelas >= 2),
    CONSTRAINT ck_divida_status CHECK (status IN ('ATIVA', 'QUITADA')),
    -- Parcelamento de cartão exige o cartão; financiamento e empréstimo podem tê-lo ou não.
    CONSTRAINT ck_divida_parcelamento_exige_cartao
        CHECK (tipo <> 'PARCELAMENTO_CARTAO' OR cartao_id IS NOT NULL),
    -- A data de quitação existe se, e somente se, a dívida está quitada.
    CONSTRAINT ck_divida_quitada_tem_data CHECK ((status = 'QUITADA') = (data_quitacao IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
