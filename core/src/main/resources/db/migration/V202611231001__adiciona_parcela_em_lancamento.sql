-- Colunas de parcela em `lancamento` (RF12). Modelo de dados, bloco B, seção 3.7.
-- Cada parcela de uma dívida é um lançamento comum, ligado a ela pelo par
-- (`divida_id`, `numero_parcela`). Um lançamento avulso tem as duas colunas nulas.
ALTER TABLE lancamento
    ADD COLUMN divida_id      BIGINT   NULL AFTER fatura_id,
    ADD COLUMN numero_parcela SMALLINT NULL AFTER divida_id,
    ADD CONSTRAINT fk_lancamento_divida FOREIGN KEY (divida_id) REFERENCES divida (id),
    ADD CONSTRAINT ck_lancamento_numero_parcela CHECK (numero_parcela >= 1),
    -- Ambos nulos (lançamento avulso) ou ambos preenchidos (parcela).
    ADD CONSTRAINT ck_lancamento_divida_e_parcela_juntas
        CHECK ((divida_id IS NULL) = (numero_parcela IS NULL)),
    -- Não existem duas parcelas com o mesmo número na mesma dívida. O índice também serve à
    -- busca das parcelas de uma dívida (saldo devedor), que filtra por `divida_id`.
    ADD CONSTRAINT uk_lancamento_divida_parcela UNIQUE (divida_id, numero_parcela);
