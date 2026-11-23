-- Ajuste manual da reserva (RF18). Modelo de dados, bloco D, seção 5.5.
--
-- A reserva em si não é gravada: é calculada pela visão `visao_reserva_mensal` (ajustes +
-- resultados dos meses). O primeiro ajuste é o saldo existente antes do uso do sistema.
--
-- Exceção à C4: o valor pode ser negativo (corrige a reserva para baixo ou registra um saldo
-- inicial negativo); só o zero é recusado, porque não ajustaria nada.
CREATE TABLE ajuste_reserva (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    data           DATE          NOT NULL,
    valor          DECIMAL(12,2) NOT NULL,
    motivo         VARCHAR(200)  NOT NULL,
    criado_em      DATETIME(3)   NOT NULL,
    criado_por     VARCHAR(50)   NOT NULL,
    atualizado_em  DATETIME(3)   NOT NULL,
    atualizado_por VARCHAR(50)   NOT NULL,
    versao         BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_ajuste_reserva_valor_diferente_de_zero CHECK (valor <> 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
