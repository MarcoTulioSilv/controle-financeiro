package com.marcotulio.financeiro.cartao;

import java.util.Objects;

/** As regras de faturamento de um cartão, sem nenhum dado do banco ou do titular. */
public record RegraCartao(int diaFechamento, int diaVencimento, CompraNoFechamento compraNoFechamento) {

    public RegraCartao {
        exigirDiaDoMes(diaFechamento, "dia de fechamento");
        exigirDiaDoMes(diaVencimento, "dia de vencimento");
        Objects.requireNonNull(compraNoFechamento, "a regra de compra no dia do fechamento é obrigatória");
    }

    /** O dia configurado é o dia do mês do cartão: meses mais curtos são tratados no cálculo (RF14). */
    private static void exigirDiaDoMes(int dia, String nome) {
        if (dia < 1 || dia > 31) {
            throw new IllegalArgumentException("o " + nome + " deve estar entre 1 e 31, mas era " + dia);
        }
    }
}