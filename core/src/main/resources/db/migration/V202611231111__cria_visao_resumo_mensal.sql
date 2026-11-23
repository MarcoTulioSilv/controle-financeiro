-- Visão para o Power BI (ADR-007): o resumo de cada mês de referência (RF09), com a economia
-- calculada pela fórmula do RF04 e a meta de economia vigente no mês.
--
-- Uma linha por mês de referência que tenha lançamento, inclusive meses futuros (parcelas já
-- lançadas). Lançamentos excluídos logicamente (ADR-006) não contam.
--
-- Economia do mês (RF04) = (resultado + compras de ativos − vendas de ativos) ÷ receitas,
-- em percentual com duas casas. Assim a troca de dinheiro por ativo (RF17) não distorce a
-- meta: comprar um ativo reduz o resultado, mas não é gasto. Compras de ativos são as despesas
-- da categoria Investimentos, e vendas, as receitas dela (ERS, Apêndice A). Sem receita no mês
-- a divisão não existe, e o percentual fica nulo.
--
-- A meta segue a vigência (modelo de dados, 5.4): vale a linha mais recente de
-- `meta_economia` com `vigente_desde` menor ou igual ao mês; antes da primeira, é nula.
--
-- O saldo inicial e o final do mês (RF09) estão em `visao_reserva_mensal`.
CREATE VIEW visao_resumo_mensal AS
WITH totais AS (
    SELECT l.mes_referencia,
           SUM(CASE WHEN l.tipo = 'RECEITA' THEN l.valor ELSE 0 END) AS receitas,
           SUM(CASE WHEN l.tipo = 'DESPESA' THEN l.valor ELSE 0 END) AS despesas,
           SUM(CASE WHEN l.tipo = 'DESPESA' AND c.nome = 'Investimentos' THEN l.valor ELSE 0 END)
               AS compras_ativos,
           SUM(CASE WHEN l.tipo = 'RECEITA' AND c.nome = 'Investimentos' THEN l.valor ELSE 0 END)
               AS vendas_ativos
      FROM lancamento l
      JOIN categoria c ON c.id = l.categoria_id
     WHERE l.excluido_em IS NULL
     GROUP BY l.mes_referencia
)
SELECT t.mes_referencia,
       t.receitas,
       t.despesas,
       t.receitas - t.despesas AS resultado,
       t.compras_ativos,
       t.vendas_ativos,
       CASE WHEN t.receitas > 0
            THEN ROUND((t.receitas - t.despesas + t.compras_ativos - t.vendas_ativos) * 100
                       / t.receitas, 2)
       END AS percentual_economia,
       (SELECT m.percentual
          FROM meta_economia m
         WHERE m.vigente_desde <= t.mes_referencia
         ORDER BY m.vigente_desde DESC
         LIMIT 1) AS meta_economia
  FROM totais t;
