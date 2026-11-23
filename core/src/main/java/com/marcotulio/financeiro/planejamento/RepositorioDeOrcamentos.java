package com.marcotulio.financeiro.planejamento;

import java.time.YearMonth;
import org.springframework.data.repository.Repository;

/** Acesso ao banco dos orçamentos. Sem exclusão: só o que está declarado aqui existe. */
interface RepositorioDeOrcamentos extends Repository<Orcamento, Long> {

    Orcamento save(Orcamento orcamento);

    boolean existsByCategoriaIdAndVigenteDesde(Long categoriaId, YearMonth vigenteDesde);
}
