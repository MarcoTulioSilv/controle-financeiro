package com.marcotulio.financeiro.lancamento;

/**
 * Por onde o lançamento entrou no sistema.
 *
 * <p>O banco também aceita {@code BOT} (modelo de dados, seção 2.7), mas o bot só chega no MVP 2:
 * até lá, deixar o valor fora do {@code enum} impede, já na compilação, que alguém registre um
 * lançamento como se viesse do WhatsApp.
 */
public enum OrigemLancamento {
    WEB,
    IMPORTACAO
}
