-- Lançamento: uma receita ou uma despesa (RF01). Modelo de dados, bloco A (seção 2.7) e as
-- colunas de cartão e fatura do bloco B (seção 3.7). As colunas de dívida (divida_id,
-- numero_parcela) chegam com a T07, e a de recorrência (recorrencia_id) com o RF03.
--
-- O valor é sempre positivo: o sentido vem do `tipo` (C4, ADR-005).
-- `destinatario_id` é o "para quem" (RF13): obrigatório e sem valor padrão.
-- `mensagem_original` só é preenchida pelo bot (fora do MVP 1).
-- `revisar` é a marca do RF15; `excluido_em` é a exclusão lógica (ADR-006). Nenhuma das duas é
-- usada no MVP 1, mas já fazem parte da tabela do modelo.
CREATE TABLE lancamento (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tipo              VARCHAR(10)   NOT NULL,
    valor             DECIMAL(12,2) NOT NULL,
    descricao         VARCHAR(200)  NOT NULL,
    categoria_id      BIGINT        NOT NULL,
    destinatario_id   BIGINT        NOT NULL,
    data_compra       DATE          NOT NULL,
    mes_referencia    DATE          NOT NULL,
    origem            VARCHAR(15)   NOT NULL,
    mensagem_original TEXT          NULL,
    revisar           BOOLEAN       NOT NULL DEFAULT FALSE,
    excluido_em       DATETIME(3)   NULL,
    cartao_id         BIGINT        NULL,
    fatura_id         BIGINT        NULL,
    criado_em         DATETIME(3)   NOT NULL,
    criado_por        VARCHAR(50)   NOT NULL,
    atualizado_em     DATETIME(3)   NOT NULL,
    atualizado_por    VARCHAR(50)   NOT NULL,
    versao            BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lancamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_lancamento_destinatario FOREIGN KEY (destinatario_id) REFERENCES destinatario (id),
    CONSTRAINT fk_lancamento_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT fk_lancamento_fatura FOREIGN KEY (fatura_id) REFERENCES fatura (id),
    CONSTRAINT ck_lancamento_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_lancamento_valor_positivo CHECK (valor > 0),
    CONSTRAINT ck_lancamento_mes_referencia_dia_1 CHECK (DAY(mes_referencia) = 1),
    CONSTRAINT ck_lancamento_origem CHECK (origem IN ('WEB', 'BOT', 'IMPORTACAO')),
    CONSTRAINT ck_lancamento_fatura_exige_cartao CHECK (fatura_id IS NULL OR cartao_id IS NOT NULL),
    -- Resumo mensal (receitas e despesas de um mês de referência).
    INDEX idx_lancamento_mes (mes_referencia),
    -- Planejado × real por categoria em um mês.
    INDEX idx_lancamento_categoria_mes (categoria_id, mes_referencia)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
