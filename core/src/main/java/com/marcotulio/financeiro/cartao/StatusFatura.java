package com.marcotulio.financeiro.cartao;

/**
 * Situação de uma fatura. No MVP 1 toda fatura nasce e permanece {@code ABERTA}: os demais
 * estados do modelo ({@code EM_REVISAO}, {@code CONFERIDA}, {@code PAGA}) já são aceitos pelo
 * banco, mas só entram aqui com a revisão de fatura (RF15, {@code maquinas-estado.md}).
 */
public enum StatusFatura {
    ABERTA
}
