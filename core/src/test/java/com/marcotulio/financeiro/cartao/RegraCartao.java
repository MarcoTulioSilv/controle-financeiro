package com.marcotulio.financeiro.cartao;

/** As regras de faturamento de um cartão, sem nenhum dado do banco ou do titular. */
public record RegraCartao(int diaFechamento, int diaVencimento, CompraNoFechamento compraNoFechamento) {
}