-- Visão para o Power BI (ADR-007): a reserva acumulada mês a mês (RF18), que é também o saldo
-- inicial e o final de cada mês no resumo do RF09.
--
-- A reserva é calculada, nunca gravada (modelo de dados, 5.5):
--   reserva no fim do mês M = soma dos ajustes até M + soma dos resultados (receitas −
--   despesas) de todos os meses de referência até M.
--   reserva no início de M  = reserva no fim do mês anterior.
-- Um ajuste conta no mês da sua data. Lançamentos excluídos logicamente (ADR-006) não contam.
--
-- Só aparecem os meses até o mês em andamento, inclusive: lançamentos (e ajustes) de meses
-- futuros só afetam a reserva quando o mês deles chegar. Meses sem nenhum lançamento nem
-- ajuste não geram linha (a reserva deles é a do último mês listado).
--
-- O "mês em andamento" é o de São Paulo. O servidor MySQL roda em UTC, e as tabelas de fusos
-- nomeados ('America/Sao_Paulo') não vêm instaladas na imagem oficial; por isso o deslocamento
-- fixo de −03:00, válido desde que o Brasil deixou de ter horário de verão (2019).
CREATE VIEW visao_reserva_mensal AS
WITH resultado_do_mes AS (
    SELECT mes_referencia,
           SUM(CASE WHEN tipo = 'RECEITA' THEN valor ELSE -valor END) AS resultado
      FROM lancamento
     WHERE excluido_em IS NULL
     GROUP BY mes_referencia
),
ajustes_do_mes AS (
    -- O mês da data do ajuste, como DATE com dia 1 (C5).
    SELECT DATE_SUB(data, INTERVAL DAY(data) - 1 DAY) AS mes_referencia,
           SUM(valor) AS ajustes
      FROM ajuste_reserva
     GROUP BY DATE_SUB(data, INTERVAL DAY(data) - 1 DAY)
),
meses AS (
    SELECT mes_referencia FROM resultado_do_mes
    UNION
    SELECT mes_referencia FROM ajustes_do_mes
),
movimento_do_mes AS (
    SELECT m.mes_referencia,
           COALESCE(a.ajustes, 0)   AS ajustes_mes,
           COALESCE(r.resultado, 0) AS resultado_mes
      FROM meses m
      LEFT JOIN ajustes_do_mes a   ON a.mes_referencia = m.mes_referencia
      LEFT JOIN resultado_do_mes r ON r.mes_referencia = m.mes_referencia
     WHERE m.mes_referencia <= LAST_DAY(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '-03:00'))
)
-- Soma acumulada com função de janela: para cada mês, soma o movimento dele e de todos os
-- anteriores (ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW).
SELECT mes_referencia,
       ajustes_mes,
       resultado_mes,
       SUM(ajustes_mes + resultado_mes) OVER acumulado - (ajustes_mes + resultado_mes)
           AS reserva_inicio_mes,
       SUM(ajustes_mes + resultado_mes) OVER acumulado AS reserva_fim_mes
  FROM movimento_do_mes
WINDOW acumulado AS (ORDER BY mes_referencia ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW);
