package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.time.YearMonth;

/** Resultado do cálculo: em qual fatura a compra entra (RN03). */
public record FaturaCalculada(YearMonth mesReferencia, LocalDate fechamento, LocalDate vencimento) {
}