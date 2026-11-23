package com.marcotulio.financeiro.planejamento;

import com.marcotulio.financeiro.cadastro.TipoCategoria;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * A parte do arquivo {@code config/dados-iniciais.yml} que pertence ao planejamento. O formato
 * está documentado no exemplo versionado, {@code config/dados-iniciais.example.yml}.
 */
record DadosIniciaisDoPlanejamento(
        @DefaultValue List<OrcamentoInicial> orcamentos,
        @DefaultValue List<MetaEconomiaInicial> metasEconomia) {

    /** A categoria é identificada como no cadastro: pelo nome e pelo tipo. */
    record OrcamentoInicial(
            String categoria,
            TipoCategoria tipo,
            YearMonth vigenteDesde,
            BigDecimal valorPlanejado) {
    }

    record MetaEconomiaInicial(YearMonth vigenteDesde, BigDecimal percentual) {
    }
}
