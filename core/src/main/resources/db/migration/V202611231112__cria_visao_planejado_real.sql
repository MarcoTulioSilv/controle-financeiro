-- Visão para o Power BI (ADR-007): planejado × real × diferença por categoria e mês de
-- referência (RF04, RF09).
--
-- Vigência (modelo de dados, 5.3): o planejado de uma categoria num mês é o da linha de
-- `orcamento` mais recente com `vigente_desde` menor ou igual ao mês. Mudar o orçamento não
-- altera os meses anteriores.
--
-- Os meses considerados são os que têm lançamento ou início de vigência de um orçamento. Para
-- cada um, aparece toda categoria com orçamento vigente (mesmo sem lançamento: real zero) ou com
-- lançamento (mesmo sem orçamento: planejado zero). Lançamentos excluídos logicamente
-- (ADR-006) não contam.
--
-- diferenca = planejado − real. Na despesa, negativo é gasto acima do planejado; na receita,
-- negativo é receita acima do planejado.
CREATE VIEW visao_planejado_real AS
WITH real_do_mes AS (
    SELECT categoria_id, mes_referencia, SUM(valor) AS valor_real
      FROM lancamento
     WHERE excluido_em IS NULL
     GROUP BY categoria_id, mes_referencia
),
meses AS (
    SELECT mes_referencia FROM lancamento WHERE excluido_em IS NULL
    UNION
    SELECT vigente_desde FROM orcamento
),
planejado_do_mes AS (
    SELECT m.mes_referencia, o.categoria_id, o.valor_planejado
      FROM meses m
      JOIN orcamento o
        ON o.vigente_desde = (SELECT MAX(vigente.vigente_desde)
                                FROM orcamento vigente
                               WHERE vigente.categoria_id = o.categoria_id
                                 AND vigente.vigente_desde <= m.mes_referencia)
),
linhas AS (
    SELECT mes_referencia, categoria_id FROM planejado_do_mes
    UNION
    SELECT mes_referencia, categoria_id FROM real_do_mes
)
SELECT li.mes_referencia,
       c.id   AS categoria_id,
       c.nome AS categoria,
       c.tipo,
       COALESCE(p.valor_planejado, 0)                            AS valor_planejado,
       COALESCE(r.valor_real, 0)                                 AS valor_real,
       COALESCE(p.valor_planejado, 0) - COALESCE(r.valor_real, 0) AS diferenca
  FROM linhas li
  JOIN categoria c ON c.id = li.categoria_id
  LEFT JOIN planejado_do_mes p
         ON p.mes_referencia = li.mes_referencia AND p.categoria_id = li.categoria_id
  LEFT JOIN real_do_mes r
         ON r.mes_referencia = li.mes_referencia AND r.categoria_id = li.categoria_id;
