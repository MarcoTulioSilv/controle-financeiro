package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Acesso ao banco dos feriados. Sem exclusão: só o que está declarado aqui existe. */
interface RepositorioDeFeriados extends Repository<Feriado, Long> {

    Feriado save(Feriado feriado);

    boolean existsByData(LocalDate data);

    /** Só as datas: para achar o próximo dia útil, a descrição não importa. */
    @Query("select f.data from Feriado f where f.data between :inicio and :fim")
    Set<LocalDate> datasEntre(LocalDate inicio, LocalDate fim);
}
