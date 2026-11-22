package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Um lançamento gravado, como os outros módulos o enxergam: só os dados, sem a entidade JPA (M5).
 *
 * @param valor sempre positivo e com escala 2; o sentido vem do {@code tipo}
 * @param destinatarioId o "para quem" (RF13)
 * @param cartaoId {@code null} quando a compra não foi no cartão
 * @param faturaId {@code null} sem cartão, ou quando o mês informado difere do mês da fatura
 */
public record LancamentoRegistrado(
        long id,
        TipoLancamento tipo,
        BigDecimal valor,
        String descricao,
        long categoriaId,
        long destinatarioId,
        LocalDate dataCompra,
        YearMonth mesReferencia,
        Long cartaoId,
        Long faturaId,
        OrigemLancamento origem) {
}
