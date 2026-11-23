package com.marcotulio.financeiro.planejamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Orçamento e meta de economia gravados pelo serviço público do planejamento (RF04). */
@Transactional
class ServicoPlanejamentoTest extends TesteDeIntegracao {

    private static final YearMonth OUT_2026 = YearMonth.of(2026, 10);

    @Autowired
    private ServicoPlanejamento servico;

    @Autowired
    private ServicoCategorias categorias;

    @Autowired
    private JdbcClient jdbc;

    private long alimentacao;

    @BeforeEach
    void cadastraUmaCategoria() {
        alimentacao = categorias.cadastrarSeAusente("Alimentação", TipoCategoria.DESPESA, true).id();
    }

    // ----- orçamento -----

    @Test
    void gravaOOrcamentoComDiaUmEEscalaDois() {
        // Act
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, new BigDecimal("1200.5"));

        // Assert
        assertThat(orcamentos()).containsExactly("2026-10-01 1200.50");
    }

    @Test
    void orcamentoJaDefinidoParaOMesmoInicioNaoEhAlterado() {
        // Arrange
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, new BigDecimal("1200.00"));

        // Act
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, new BigDecimal("9999.00"));

        // Assert
        assertThat(orcamentos()).containsExactly("2026-10-01 1200.00");
    }

    @Test
    void novaVigenciaEhUmaLinhaNovaEPreservaAAnterior() {
        // Arrange
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, new BigDecimal("1200.00"));

        // Act
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026.plusMonths(1), new BigDecimal("1300.00"));

        // Assert
        assertThat(orcamentos()).containsExactly("2026-10-01 1200.00", "2026-11-01 1300.00");
    }

    @Test
    void orcamentoAceitaZero() {
        // Act
        servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, BigDecimal.ZERO);

        // Assert
        assertThat(orcamentos()).containsExactly("2026-10-01 0.00");
    }

    @Test
    void orcamentoRecusaValorNegativoOuComFracaoDeCentavo() {
        // Act e Assert
        assertThatThrownBy(() -> servico.definirOrcamentoSeAusente(
                        alimentacao, OUT_2026, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor planejado não pode ser negativo.");
        assertThatThrownBy(() -> servico.definirOrcamentoSeAusente(
                        alimentacao, OUT_2026, new BigDecimal("10.005")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor planejado aceita no máximo dois decimais (centavos).");
    }

    @Test
    void orcamentoExigeValorEInicioDaVigencia() {
        // Act e Assert
        assertThatThrownBy(() -> servico.definirOrcamentoSeAusente(alimentacao, OUT_2026, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o valor planejado.");
        assertThatThrownBy(() -> servico.definirOrcamentoSeAusente(
                        alimentacao, null, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o mês de início da vigência.");
    }

    // ----- meta de economia -----

    @Test
    void gravaAMetaDeEconomia() {
        // Act
        servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("10"));

        // Assert
        assertThat(metas()).containsExactly("2026-10-01 10.00");
    }

    @Test
    void metaJaDefinidaParaOMesmoInicioNaoEhAlterada() {
        // Arrange
        servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("10.00"));

        // Act
        servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("50.00"));

        // Assert
        assertThat(metas()).containsExactly("2026-10-01 10.00");
    }

    @Test
    void metaAceitaOsLimitesZeroECem() {
        // Act
        servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("0"));
        servico.definirMetaDeEconomiaSeAusente(OUT_2026.plusMonths(1), new BigDecimal("100"));

        // Assert
        assertThat(metas()).containsExactly("2026-10-01 0.00", "2026-11-01 100.00");
    }

    @Test
    void metaRecusaPercentualForaDeZeroACem() {
        // Act e Assert
        assertThatThrownBy(() -> servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A meta de economia deve ficar entre 0% e 100%.");
        assertThatThrownBy(() -> servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A meta de economia deve ficar entre 0% e 100%.");
    }

    @Test
    void metaRecusaMaisDeDuasCasasOuValorAusente() {
        // Act e Assert
        assertThatThrownBy(() -> servico.definirMetaDeEconomiaSeAusente(OUT_2026, new BigDecimal("10.555")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A meta de economia aceita no máximo duas casas decimais.");
        assertThatThrownBy(() -> servico.definirMetaDeEconomiaSeAusente(OUT_2026, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o percentual da meta de economia.");
    }

    // ----- apoio -----

    private List<String> orcamentos() {
        return jdbc.sql("SELECT vigente_desde, valor_planejado FROM orcamento ORDER BY vigente_desde")
                .query((rs, n) -> rs.getString(1) + " " + rs.getString(2))
                .list();
    }

    private List<String> metas() {
        return jdbc.sql("SELECT vigente_desde, percentual FROM meta_economia ORDER BY vigente_desde")
                .query((rs, n) -> rs.getString(1) + " " + rs.getString(2))
                .list();
    }
}
