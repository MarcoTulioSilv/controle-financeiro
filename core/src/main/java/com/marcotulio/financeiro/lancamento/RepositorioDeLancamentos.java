package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * A soma das parcelas da dívida com mês de referência posterior a {@code mes} (o dia 1 de um
     * mês de referência). É o saldo devedor ({@code docs/design/maquinas-estado.md}, seção 2).
     *
     * <p>SQL nativo porque a entidade não mapeia {@code excluido_em} (sem uso no MVP 1), e as
     * parcelas excluídas logicamente pelo adiantamento (T16) não podem entrar no saldo.
     */
    @Query(value = """
            SELECT COALESCE(SUM(valor), 0)
              FROM lancamento
             WHERE divida_id = :dividaId
               AND mes_referencia > :mes
               AND excluido_em IS NULL
            """, nativeQuery = true)
    BigDecimal somarParcelasPosterioresA(long dividaId, LocalDate mes);
}
