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

    /**
     * A fatura de um mês de referência conhecido, sem uma data de compra: é o caso das parcelas
     * 2, 3... de uma compra parcelada, que entram nas faturas dos meses seguintes (RF12).
     *
     * <p>O vencimento é o dia do cartão no próprio mês. O fechamento é o último dia de fechamento
     * antes do vencimento: no mesmo mês, se vier antes do vencimento; senão, no mês anterior.
     */
    public FaturaCalculada calcularPeloMes(RegraCartao regra, YearMonth mesReferencia) {
        LocalDate vencimento = diaDoMes(mesReferencia, regra.diaVencimento());
        LocalDate fechamento = diaDoMes(mesReferencia, regra.diaFechamento());
        if (!fechamento.isBefore(vencimento)) {
            fechamento = diaDoMes(mesReferencia.minusMonths(1), regra.diaFechamento());
        }
        return new FaturaCalculada(mesReferencia, fechamento, vencimento);
    }

    /** A primeira data, a partir de {@code aPartirDe} (inclusive), com o dia do mês {@code dia}. */
    private static LocalDate proximaDataComDia(int dia, LocalDate aPartirDe) {
        YearMonth mes = YearMonth.from(aPartirDe);
        LocalDate candidata = diaDoMes(mes, dia);
        if (candidata.isBefore(aPartirDe)) {
            // O mês seguinte é recalculado do dia configurado, e não da data já ajustada: somar
            // um mês a 28/02 (encurtado de 31) devolveria 28/03, perdendo o dia do cartão.
            candidata = diaDoMes(mes.plusMonths(1), dia);
        }
        return candidata;
    }

    /** O dia {@code dia} de {@code mes}; se ele não existir nesse mês, o último dia dele (RF14). */
    private static LocalDate diaDoMes(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }
}