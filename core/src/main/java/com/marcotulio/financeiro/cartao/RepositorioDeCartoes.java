package com.marcotulio.financeiro.cartao;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Acesso ao banco dos cartões. Sem exclusão: um cartão cancelado é desativado, porque as faturas
 * antigas continuam apontando para ele.
 */
interface RepositorioDeCartoes extends Repository<Cartao, Long> {

    Cartao save(Cartao cartao);

    Optional<Cartao> findById(Long id);

    /** A comparação segue a collation da coluna: {@code BMT} encontra {@code bmt}. */
    Optional<Cartao> findByApelido(String apelido);
}
