package com.marcotulio.financeiro.lancamento;

import org.springframework.data.repository.Repository;

/**
 * Acesso ao banco das dívidas. Como o dos lançamentos, estende {@link Repository} para não
 * oferecer exclusão: só o que está declarado aqui existe.
 */
interface RepositorioDeDividas extends Repository<Divida, Long> {

    Divida save(Divida divida);

    boolean existsById(Long id);
}
