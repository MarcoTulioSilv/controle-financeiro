package com.marcotulio.financeiro.lancamento;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Acesso ao banco dos lançamentos.
 *
 * <p>Estende {@link Repository} (e não {@code JpaRepository}, que traria {@code delete} pronto):
 * no MVP 1 não há exclusão de lançamentos, e depois dele a exclusão será lógica (ADR-006), sempre
 * com a validação cruzada (RF10).
 */
interface RepositorioDeLancamentos extends Repository<Lancamento, Long> {

    Lancamento save(Lancamento lancamento);

    Optional<Lancamento> findById(Long id);
}
