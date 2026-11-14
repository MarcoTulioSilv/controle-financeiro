package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.time.YearMonth;

/** Calcula a fatura em que uma compra no cartão entra (RN03). Função pura: sem banco, sem relógio. */
public final class CalculadoraFatura {

    public FaturaCalculada calcular(RegraCartao regra, LocalDate dataCompra) {
        LocalDate fechamento = proximaDataComDia(regra.diaFechamento(), dataCompra);
        if (fechamento.isEqual(dataCompra) && regra.compraNoFechamento() == CompraNoFechamento.PROXIMA) {
            // A compra caiu no próprio dia do fechamento e o cartão a joga para a fatura seguinte.
            fechamento = proximaDataComDia(regra.diaFechamento(), fechamento.plusDays(1));
        }
        LocalDate vencimento = proximaDataComDia(regra.diaVencimento(), fechamento.plusDays(1));
        return new FaturaCalculada(YearMonth.from(vencimento), fechamento, vencimento);
    }

    /** A primeira data, a partir de {@code aPartirDe} (inclusive), cujo dia do mês é {@code dia}. */
    private static LocalDate proximaDataComDia(int dia, LocalDate aPartirDe) {
        LocalDate candidata = comDia(aPartirDe, dia);
        return candidata.isBefore(aPartirDe) ? candidata.plusMonths(1) : candidata;
    }

    /** O dia {@code dia} do mês de {@code base}; se ele não existir, o último dia desse mês (RF14). */
    private static LocalDate comDia(LocalDate base, int dia) {
        return base.withDayOfMonth(Math.min(dia, base.lengthOfMonth()));
    }
}