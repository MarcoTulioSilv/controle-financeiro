package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Uma fatura gravada, como os outros módulos a enxergam (M5).
 *
 * @param dataVencimento vencimento nominal: define o mês de referência (RF14)
 * @param dataVencimentoEfetiva primeiro dia útil a partir do nominal (RF16), para os avisos
 */
public record FaturaRegistrada(
        long id,
        long cartaoId,
        YearMonth mesReferencia,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        LocalDate dataVencimentoEfetiva,
        StatusFatura status) {
}
