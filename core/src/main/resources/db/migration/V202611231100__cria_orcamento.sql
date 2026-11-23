-- Orçamento: o valor planejado de uma categoria (de receita ou de despesa) a partir de um mês
-- (RF04). Modelo de dados, bloco D, seção 5.3.
--
-- Dado com vigência: o planejado de um mês é a linha mais recente com `vigente_desde` menor ou
-- igual ao mês. Mudar o orçamento é gravar uma linha nova; os meses anteriores mantêm o valor
-- que tinham. Zero é aceito: é deixar de planejar a categoria a partir daquele mês.
CREATE TABLE orcamento (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    categoria_id    BIGINT        NOT NULL,
    vigente_desde   DATE          NOT NULL,
    valor_planejado DECIMAL(12,2) NOT NULL,
    criado_em       DATETIME(3)   NOT NULL,
    criado_por      VARCHAR(50)   NOT NULL,
    atualizado_em   DATETIME(3)   NOT NULL,
    atualizado_por  VARCHAR(50)   NOT NULL,
    versao          BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orcamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT uk_orcamento_categoria_vigencia UNIQUE (categoria_id, vigente_desde),
    CONSTRAINT ck_orcamento_vigente_desde_dia_1 CHECK (DAY(vigente_desde) = 1),
    CONSTRAINT ck_orcamento_valor_planejado CHECK (valor_planejado >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
