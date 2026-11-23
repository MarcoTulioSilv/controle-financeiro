package com.marcotulio.financeiro.planejamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restrições das tabelas {@code orcamento}, {@code meta_economia} e {@code ajuste_reserva}
 * (bloco D de {@code docs/design/modelo-dados.md}) garantidas pelo próprio banco.
 *
 * <p>Os testes gravam com SQL direto, sem passar pelo serviço, para provar que a regra está no
 * schema. {@code @Transactional} desfaz cada teste ao final.
 */
@Transactional
class RestricoesDoPlanejamentoTest extends TesteDeIntegracao {

    @Autowired
    private JdbcClient jdbc;

    private long alimentacao;

    @BeforeEach
    void cadastraUmaCategoria() {
        jdbc.sql("INSERT INTO categoria (nome, tipo, %s) VALUES ('Alimentação', 'DESPESA', %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .update();
        alimentacao = ultimoIdGerado();
    }

    // ----- orcamento -----

    @Test
    void orcamentoAceitaValorPlanejadoZero() {
        // Act: planejar zero é deixar de planejar a categoria a partir daquele mês.
        inserirOrcamento(alimentacao, "2026-10-01", "0.00");

        // Assert
        assertThat(contar("SELECT COUNT(*) FROM orcamento")).isEqualTo(1);
    }

    @Test
    void orcamentoRecusaValorPlanejadoNegativo() {
        // Act e Assert
        assertThatThrownBy(() -> inserirOrcamento(alimentacao, "2026-10-01", "-0.01"))
                .satisfies(violou("ck_orcamento_valor_planejado"));
    }

    @Test
    void orcamentoVigoraSempreDesdeODia1() {
        // Act e Assert
        assertThatThrownBy(() -> inserirOrcamento(alimentacao, "2026-10-02", "100.00"))
                .satisfies(violou("ck_orcamento_vigente_desde_dia_1"));
    }

    @Test
    void umaCategoriaTemUmSoOrcamentoPorInicioDeVigencia() {
        // Arrange
        inserirOrcamento(alimentacao, "2026-10-01", "100.00");

        // Act e Assert
        assertThatThrownBy(() -> inserirOrcamento(alimentacao, "2026-10-01", "200.00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void orcamentoExigeCategoriaExistente() {
        // Act e Assert
        assertThatThrownBy(() -> inserirOrcamento(alimentacao + 1000, "2026-10-01", "100.00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- meta_economia -----

    @Test
    void metaAceitaDeZeroACemPorCento() {
        // Act
        inserirMeta("2026-01-01", "0.00");
        inserirMeta("2026-02-01", "100.00");

        // Assert
        assertThat(contar("SELECT COUNT(*) FROM meta_economia")).isEqualTo(2);
    }

    @Test
    void metaRecusaPercentualForaDeZeroACem() {
        // Act e Assert
        assertThatThrownBy(() -> inserirMeta("2026-01-01", "100.01"))
                .satisfies(violou("ck_meta_economia_percentual"));
        assertThatThrownBy(() -> inserirMeta("2026-01-01", "-0.01"))
                .satisfies(violou("ck_meta_economia_percentual"));
    }

    @Test
    void metaVigoraSempreDesdeODia1() {
        // Act e Assert
        assertThatThrownBy(() -> inserirMeta("2026-01-15", "10.00"))
                .satisfies(violou("ck_meta_economia_vigente_desde_dia_1"));
    }

    @Test
    void existeUmaSoMetaPorInicioDeVigencia() {
        // Arrange
        inserirMeta("2026-01-01", "10.00");

        // Act e Assert
        assertThatThrownBy(() -> inserirMeta("2026-01-01", "15.00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- ajuste_reserva -----

    @Test
    void ajusteDaReservaPodeSerNegativo() {
        // Act: exceção à C4, um ajuste corrige a reserva para baixo.
        inserirAjuste("2026-09-30", "-250.00");

        // Assert
        String valor = jdbc.sql("SELECT valor FROM ajuste_reserva").query(String.class).single();
        assertThat(valor).isEqualTo("-250.00");
    }

    @Test
    void ajusteDaReservaNaoPodeSerZero() {
        // Act e Assert
        assertThatThrownBy(() -> inserirAjuste("2026-09-30", "0.00"))
                .satisfies(violou("ck_ajuste_reserva_valor_diferente_de_zero"));
    }

    // ----- apoio -----

    private void inserirOrcamento(long categoria, String vigenteDesde, String valorPlanejado) {
        jdbc.sql("""
                        INSERT INTO orcamento (categoria_id, vigente_desde, valor_planejado, %s)
                        VALUES (?, ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(categoria, vigenteDesde, valorPlanejado)
                .update();
    }

    private void inserirMeta(String vigenteDesde, String percentual) {
        jdbc.sql("INSERT INTO meta_economia (vigente_desde, percentual, %s) VALUES (?, ?, %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(vigenteDesde, percentual)
                .update();
    }

    private void inserirAjuste(String data, String valor) {
        jdbc.sql("INSERT INTO ajuste_reserva (data, valor, motivo, %s) VALUES (?, ?, 'teste', %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(data, valor)
                .update();
    }

    private int contar(String consulta) {
        return jdbc.sql(consulta).query(Integer.class).single();
    }

    private long ultimoIdGerado() {
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    private static ThrowingConsumer<Throwable> violou(String restricao) {
        return erro -> assertThat(erro)
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint '" + restricao + "' is violated");
    }

    private static final String COLUNAS_C7 =
            "criado_em, criado_por, atualizado_em, atualizado_por, versao";

    private static final String VALORES_C7 =
            "UTC_TIMESTAMP(3), 'teste', UTC_TIMESTAMP(3), 'teste', 0";
}
