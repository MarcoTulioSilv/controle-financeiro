package com.marcotulio.financeiro.lancamento;

/**
 * A situação de uma dívida ({@code docs/design/maquinas-estado.md}, seção 2).
 *
 * <p>O banco também aceita {@code QUITADA}, mas no MVP 1 a dívida só existe ativa: a quitação
 * (pelo agendador, no mês da última parcela, ou por adiantamento, na T16) chega depois. Como no
 * {@link OrigemLancamento}, deixar o valor fora do {@code enum} impede, já na compilação, que
 * alguém quite uma dívida sem as regras da quitação.
 */
public enum StatusDivida {
    ATIVA
}
