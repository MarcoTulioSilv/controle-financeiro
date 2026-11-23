package com.marcotulio.financeiro.planejamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de entrada pública do planejamento: orçamento por categoria e meta de economia (RF04).
 *
 * <p>Os cálculos de planejado × real e de economia não ficam aqui: estão nas visões SQL lidas
 * pelo Power BI (ADR-007).
 */
@Service
public class ServicoPlanejamento {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final RepositorioDeOrcamentos orcamentos;
    private final RepositorioDeMetasDeEconomia metas;

    ServicoPlanejamento(RepositorioDeOrcamentos orcamentos, RepositorioDeMetasDeEconomia metas) {
        this.orcamentos = orcamentos;
        this.metas = metas;
    }

    /**
     * Define o valor planejado da categoria a partir de {@code vigenteDesde}, a menos que ela já
     * tenha um orçamento começando nesse mês: nesse caso nada muda. Assim a carga de dados
     * iniciais pode rodar de novo sem desfazer o que o usuário mudou depois.
     */
    @Transactional
    public void definirOrcamentoSeAusente(long categoriaId, YearMonth vigenteDesde, BigDecimal valorPlanejado) {
        exigir(vigenteDesde != null, "Informe o mês de início da vigência.");
        BigDecimal valor = valorPlanejadoEmCentavos(valorPlanejado);
        if (!orcamentos.existsByCategoriaIdAndVigenteDesde(categoriaId, vigenteDesde)) {
            orcamentos.save(new Orcamento(categoriaId, vigenteDesde, valor));
        }
    }

    /** Define a meta de economia a partir de {@code vigenteDesde}, se ainda não houver uma nesse mês. */
    @Transactional
    public void definirMetaDeEconomiaSeAusente(YearMonth vigenteDesde, BigDecimal percentual) {
        exigir(vigenteDesde != null, "Informe o mês de início da vigência.");
        BigDecimal conferido = percentualConferido(percentual);
        if (!metas.existsByVigenteDesde(vigenteDesde)) {
            metas.save(new MetaEconomia(vigenteDesde, conferido));
        }
    }

    /**
     * Escala 2 (ADR-005). Zero é aceito: deixa de planejar a categoria. Uma fração de centavo é
     * recusada, e não arredondada em silêncio ({@code RoundingMode.UNNECESSARY}).
     */
    private static BigDecimal valorPlanejadoEmCentavos(BigDecimal valor) {
        exigir(valor != null, "Informe o valor planejado.");
        exigir(valor.signum() >= 0, "O valor planejado não pode ser negativo.");
        // stripTrailingZeros: "150.500" tem escala 3, mas são só R$ 150,50.
        exigir(valor.stripTrailingZeros().scale() <= 2,
                "O valor planejado aceita no máximo dois decimais (centavos).");
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal percentualConferido(BigDecimal percentual) {
        exigir(percentual != null, "Informe o percentual da meta de economia.");
        exigir(percentual.signum() >= 0 && percentual.compareTo(CEM) <= 0,
                "A meta de economia deve ficar entre 0% e 100%.");
        exigir(percentual.stripTrailingZeros().scale() <= 2,
                "A meta de economia aceita no máximo duas casas decimais.");
        return percentual.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
