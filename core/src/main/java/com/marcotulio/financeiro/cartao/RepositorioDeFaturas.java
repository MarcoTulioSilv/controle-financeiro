package com.marcotulio.financeiro.cartao;

import jakarta.persistence.LockModeType;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Acesso ao banco das faturas. Sem exclusão: só o que está declarado aqui existe. */
interface RepositorioDeFaturas extends Repository<Fatura, Long> {

    /** {@code saveAndFlush}: o INSERT vai ao banco na hora, e um UNIQUE violado falha aqui. */
    Fatura saveAndFlush(Fatura fatura);

    Optional<Fatura> findByCartaoIdAndMesReferencia(Long cartaoId, YearMonth mesReferencia);

    /**
     * A mesma busca, mas com {@code SELECT ... FOR SHARE}. No MySQL, uma leitura com trava
     * enxerga a última versão confirmada da linha, e não a "fotografia" que a transação tirou na
     * primeira leitura (isolamento REPEATABLE READ). É o que permite achar a fatura que outra
     * transação acabou de criar.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select f from Fatura f where f.cartaoId = :cartaoId and f.mesReferencia = :mesReferencia")
    Optional<Fatura> buscarUltimaVersaoConfirmada(Long cartaoId, YearMonth mesReferencia);
}
