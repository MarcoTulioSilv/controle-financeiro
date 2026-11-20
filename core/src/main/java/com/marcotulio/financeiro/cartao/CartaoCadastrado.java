package com.marcotulio.financeiro.cartao;

/**
 * Um cartão como os outros módulos o enxergam: só os dados, sem a entidade JPA (M5).
 *
 * @param titularId id do usuário titular do cartão
 */
public record CartaoCadastrado(
        long id, String apelido, String nome, long titularId, RegraCartao regra, boolean ativo) {
}
