-- Visão para o Power BI (ADR-007): os lançamentos com nomes legíveis no lugar dos ids.
--
-- Serve à "visão por mês de referência (padrão) e por data da compra" e ao "filtro por para
-- quem" do RF09: as duas datas e o destinatário estão em cada linha.
--
-- Regras que ficam aqui, e não no Power BI:
-- - lançamentos excluídos logicamente (`excluido_em` preenchido, ADR-006) não aparecem;
-- - `valor_com_sinal` é positivo na receita e negativo na despesa, para que somar a coluna dê
--   o resultado (no banco o valor é sempre positivo, C4).
-- Os ids continuam na visão para o Power BI relacionar as linhas com as outras visões.
CREATE VIEW visao_lancamento AS
SELECT l.id,
       l.tipo,
       l.valor,
       CASE WHEN l.tipo = 'RECEITA' THEN l.valor ELSE -l.valor END AS valor_com_sinal,
       l.descricao,
       l.data_compra,
       l.mes_referencia,
       l.categoria_id,
       c.nome      AS categoria,
       c.ativa     AS categoria_ativa,
       l.destinatario_id,
       d.nome      AS para_quem,
       l.cartao_id,
       ct.apelido  AS cartao,
       ct.nome     AS cartao_nome,
       l.origem
  FROM lancamento l
  JOIN categoria c     ON c.id = l.categoria_id
  JOIN destinatario d  ON d.id = l.destinatario_id
  -- LEFT JOIN: lançamento sem cartão (dinheiro, Pix, débito) também aparece, com cartão nulo.
  LEFT JOIN cartao ct  ON ct.id = l.cartao_id
 WHERE l.excluido_em IS NULL;
