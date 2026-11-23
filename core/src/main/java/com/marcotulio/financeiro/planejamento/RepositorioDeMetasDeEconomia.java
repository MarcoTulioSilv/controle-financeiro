package com.marcotulio.financeiro.planejamento;

import java.time.YearMonth;
import org.springframework.data.repository.Repository;

/** Acesso ao banco das metas de economia. Sem exclusão: só o que está declarado aqui existe. */
interface RepositorioDeMetasDeEconomia extends Repository<MetaEconomia, Long> {

    MetaEconomia save(MetaEconomia meta);

    boolean existsByVigenteDesde(YearMonth vigenteDesde);
}
