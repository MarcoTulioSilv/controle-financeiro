package com.marcotulio.financeiro.lancamento;

/**
 * O que originou a dívida (RF12). Compra parcelada e dívida são a mesma entidade (modelo de
 * dados, seção 3.1): só o parcelamento de cartão exige um cartão.
 */
public enum TipoDivida {
    PARCELAMENTO_CARTAO,
    FINANCIAMENTO,
    EMPRESTIMO
}
