package com.marcotulio.financeiro.planejamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga do orçamento e da meta de economia a partir do arquivo de dados iniciais.
 *
 * <p>Como nas outras cargas, os testes usam só o exemplo versionado, com os nomes fictícios do
 * ERS, nunca o arquivo real. As categorias do orçamento são cadastradas antes, pelo serviço
 * público do cadastro, como acontece na aplicação: a carga do cadastro roda primeiro.
 */
@Transactional
class CargaPlanejamentoInicialTest extends TesteDeIntegracao {

    private static final Resource EXEMPLO =
            new FileSystemResource("../config/dados-iniciais.example.yml");

    @Autowired
    private CargaPlanejamentoInicial carga;

    @Autowired
    private ServicoPlanejamento planejamento;

    @Autowired
    private ServicoCategorias categorias;

    @Autowired
    private JdbcClient jdbc;

    private long alimentacao;

    @BeforeEach
    void cadastraAsCategoriasDoExemplo() {
        alimentacao = categorias.cadastrarSeAusente("Alimentação", TipoCategoria.DESPESA, true).id();
        categorias.cadastrarSeAusente("Lazer", TipoCategoria.DESPESA, true);
        categorias.cadastrarSeAusente("Trabalho A", TipoCategoria.RECEITA, true);
    }

    @Test
    void carregaOOrcamentoEAMetaDoExemplo() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(orcamentos()).containsExactlyInAnyOrder(
                "Alimentação DESPESA 2026-01-01 1200.00",
                "Lazer DESPESA 2026-01-01 300.00",
                "Trabalho A RECEITA 2026-01-01 5000.00");
        assertThat(metas()).containsExactly("2026-01-01 10.00");
    }

    @Test
    void rodarDuasVezesNaoDuplicaNada() throws Exception {
        // Arrange
        carga.carregar(EXEMPLO);

        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(orcamentos()).hasSize(3);
        assertThat(metas()).hasSize(1);
    }

    @Test
    void naoAlteraOOrcamentoJaGravado() throws Exception {
        // Arrange: o usuário já tinha mudado o valor de jan/2026.
        planejamento.definirOrcamentoSeAusente(alimentacao, YearMonth.of(2026, 1), new BigDecimal("900.00"));

        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(orcamentos()).contains("Alimentação DESPESA 2026-01-01 900.00");
    }

    @Test
    void semArquivoNadaECarregado() throws Exception {
        // Act
        carga.carregar(new FileSystemResource("../config/arquivo-que-nao-existe.yml"));

        // Assert
        assertThat(orcamentos()).isEmpty();
        assertThat(metas()).isEmpty();
    }

    @Test
    void orcamentoDeCategoriaQueNaoExisteInterrompeACarga() {
        // Arrange: "Viagens" é uma categoria de despesa, não de receita.
        Resource invalido = new ByteArrayResource("""
                orcamentos:
                  - categoria: Viagens
                    tipo: RECEITA
                    vigente-desde: "2026-01"
                    valor-planejado: "100.00"
                """.getBytes(StandardCharsets.UTF_8));

        // Act e Assert
        assertThatThrownBy(() -> carga.carregar(invalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Viagens");
    }

    private List<String> orcamentos() {
        return jdbc.sql("""
                        SELECT c.nome, c.tipo, o.vigente_desde, o.valor_planejado
                          FROM orcamento o JOIN categoria c ON c.id = o.categoria_id
                        """)
                .query((rs, n) -> String.join(" ",
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .list();
    }

    private List<String> metas() {
        return jdbc.sql("SELECT vigente_desde, percentual FROM meta_economia")
                .query((rs, n) -> rs.getString(1) + " " + rs.getString(2))
                .list();
    }
}
