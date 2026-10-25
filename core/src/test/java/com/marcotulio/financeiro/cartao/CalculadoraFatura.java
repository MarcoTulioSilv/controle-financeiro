package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.time.YearMonth;

/** Calcula a fatura em que uma compra no cartão entra (RN03). Função pura: sem banco, sem relógio. */
public final class CalculadoraFatura {

    public FaturaCalculada calcular(RegraCartao regra, LocalDate dataCompra) {
        LocalDate fechamento = proximaDataComDia(regra.diaFechamento(), dataCompra);
        LocalDate vencimento = proximaDataComDia(regra.diaVencimento(), fechamento.plusDays(1));
        return new FaturaCalculada(YearMonth.from(vencimento), fechamento, vencimento);
    }

    /** A primeira data, a partir de {@code aPartirDe} (inclusive), cujo dia do mês é {@code dia}. */
    private static LocalDate proximaDataComDia(int dia, LocalDate aPartirDe) {
        LocalDate candidata = aPartirDe.withDayOfMonth(dia);
        return candidata.isBefore(aPartirDe) ? candidata.plusMonths(1) : candidata;
    }
}