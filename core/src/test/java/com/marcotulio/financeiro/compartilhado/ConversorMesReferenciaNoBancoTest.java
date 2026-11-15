package com.marcotulio.financeiro.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** O conversor de mês de referência contra um MySQL de verdade, com o CHECK (DAY(col) = 1). */
class ConversorMesReferenciaNoBancoTest extends TesteDeIntegracao {

    @Autowired
    private RepositorioDeRegistroDeExemplo repositorio;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void gravaOMesDeReferenciaNoDiaUmELeDeVolta() {
        // Arrange
        RegistroDeExemplo registro =
                new RegistroDeExemplo("outubro de 2026", YearMonth.of(2026, 10));

        // Act
        RegistroDeExemplo gravado = repositorio.saveAndFlush(registro);
        LocalDate colunaNoBanco = jdbc
                .sql("SELECT mes_referencia FROM registro_de_exemplo WHERE id = ?")
                .param(gravado.getId())
                .query(LocalDate.class)
                .single();

        // Assert
        assertThat(colunaNoBanco).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(repositorio.findById(gravado.getId()).orElseThrow().getMesReferencia())
                .isEqualTo(YearMonth.of(2026, 10));
    }
}
