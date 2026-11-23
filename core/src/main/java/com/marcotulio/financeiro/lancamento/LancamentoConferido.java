package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Os dados de um lançamento depois de conferidos pelas {@link RegrasDoLancamento}: valor com
 * escala 2, descrição sem espaços nas pontas e mês de referência já decidido. É só isso que a
 * entidade aceita gravar.
 *
 * @param cartaoId {@code null} quando a compra não foi no cartão
 * @param faturaId {@code null} sem cartão, ou quando o mês informado difere do mês da fatura
 */
record LancamentoConferido(
        TipoLancamento tipo,
        BigDecimal valor,
        String descricao,
        long categoriaId,
        long destinatarioId,
        LocalDate dataCompra,
        YearMonth mesReferencia,
        Long cartaoId,
        Long faturaId) {
}
