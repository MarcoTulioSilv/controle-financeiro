package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calendário de feriados bancários gravado no banco (RF16).
 *
 * <p>Os testes usam o ano de 2030 porque a aplicação, ao subir, já grava os feriados nacionais do
 * ano corrente e do seguinte.
 */
@Transactional
class ServicoFeriadosTest extends TesteDeIntegracao {

    private static final int ANO = 2030;

    @Autowired
    private ServicoFeriados servico;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void gravaOsFeriadosNacionaisDoCalendarioBancario() {
        // Arrange
        Map<LocalDate, String> esperados = new CalendarioBancario().feriadosNacionais(ANO);

        // Act
        servico.gravarFeriadosNacionais(ANO);

        // Assert
        var gravados = jdbc.sql("""
                        SELECT data, descricao FROM feriado
                         WHERE YEAR(data) = ? AND abrangencia = 'NACIONAL'
                        """)
                .param(ANO)
                .query((linha, numero) -> Map.entry(
                        linha.getObject("data", LocalDate.class), linha.getString("descricao")))
                .list();
        assertThat(gravados).containsExactlyInAnyOrderElementsOf(esperados.entrySet());
    }

    @Test
    void gravarOsFeriadosNacionaisDuasVezesNaoDuplicaNada() {
        // Arrange
        servico.gravarFeriadosNacionais(ANO);
        int depoisDaPrimeira = contarFeriadosDoAno();

        // Act
        servico.gravarFeriadosNacionais(ANO);

        // Assert
        assertThat(contarFeriadosDoAno()).isEqualTo(depoisDaPrimeira);
    }

    @Test
    void cadastraFeriadoLocal() {
        // Arrange
        LocalDate aniversarioDaCidade = LocalDate.of(ANO, 7, 15);

        // Act
        servico.cadastrarLocalSeAusente(aniversarioDaCidade, "Aniversário da cidade");

        // Assert
        String abrangencia = jdbc.sql("SELECT abrangencia FROM feriado WHERE data = ?")
                .param(aniversarioDaCidade)
                .query(String.class)
                .single();
        assertThat(abrangencia).isEqualTo("LOCAL");
    }

    @Test
    void feriadoLocalNaDataDeOutroFeriadoNaoDuplica() {
        // Arrange: Tiradentes já está gravado como nacional.
        servico.gravarFeriadosNacionais(ANO);
        LocalDate tiradentes = LocalDate.of(ANO, 4, 21);

        // Act
        servico.cadastrarLocalSeAusente(tiradentes, "Feriado local repetido");

        // Assert: o dia continua sendo um só, e com a descrição original.
        var descricoes = jdbc.sql("SELECT descricao FROM feriado WHERE data = ?")
                .param(tiradentes)
                .query(String.class)
                .list();
        assertThat(descricoes).containsExactly("Tiradentes");
    }

    @Test
    void informaOsDiasSemExpedienteDeUmPeriodo() {
        // Arrange
        servico.gravarFeriadosNacionais(ANO);
        servico.cadastrarLocalSeAusente(LocalDate.of(ANO, 11, 25), "Feriado local de teste");

        // Act
        var dias = servico.feriadosEntre(LocalDate.of(ANO, 11, 1), LocalDate.of(ANO, 11, 30));

        // Assert: Finados, Proclamação da República, Consciência Negra e o local.
        assertThat(dias).containsExactlyInAnyOrder(
                LocalDate.of(ANO, 11, 2),
                LocalDate.of(ANO, 11, 15),
                LocalDate.of(ANO, 11, 20),
                LocalDate.of(ANO, 11, 25));
    }

    private int contarFeriadosDoAno() {
        return jdbc.sql("SELECT COUNT(*) FROM feriado WHERE YEAR(data) = ?")
                .param(ANO)
                .query(Integer.class)
                .single();
    }
}
