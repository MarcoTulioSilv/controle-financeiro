package com.marcotulio.financeiro.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Colunas padrão de auditoria e concorrência otimista (convenção C7). */
class EntidadeAuditavelTest extends TesteDeIntegracao {

    @Autowired
    private RepositorioDeRegistroDeExemplo repositorio;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void preencheQuemCriouEQuandoNaPrimeiraGravacao() {
        // Arrange
        RegistroDeExemplo registro = new RegistroDeExemplo("primeiro", YearMonth.of(2026, 10));

        // Act
        RegistroDeExemplo gravado = repositorio.saveAndFlush(registro);

        // Assert
        assertThat(gravado.getCriadoEm()).isNotNull();
        assertThat(gravado.getAtualizadoEm()).isNotNull();
        // Até a T08 (login) ninguém está autenticado: o autor é o próprio sistema.
        assertThat(gravado.getCriadoPor()).isEqualTo("sistema");
        assertThat(gravado.getAtualizadoPor()).isEqualTo("sistema");
        assertThat(gravado.getVersao()).isZero();
    }

    @Test
    void avancaAVersaoEARegistraAAtualizacaoNaEdicao() {
        // Arrange
        RegistroDeExemplo gravado =
                repositorio.saveAndFlush(new RegistroDeExemplo("antes", YearMonth.of(2026, 10)));
        Instant criadoEmOriginal = gravado.getCriadoEm();

        // Act
        gravado.setDescricao("depois");
        RegistroDeExemplo editado = repositorio.saveAndFlush(gravado);

        // Assert
        assertThat(editado.getVersao()).isEqualTo(1L);
        assertThat(editado.getCriadoEm()).isEqualTo(criadoEmOriginal);
        assertThat(editado.getAtualizadoEm()).isAfterOrEqualTo(criadoEmOriginal);
    }

    @Test
    void gravaOMomentoDaCriacaoEmUtc() {
        // Arrange
        RegistroDeExemplo gravado =
                repositorio.saveAndFlush(new RegistroDeExemplo("em utc", YearMonth.of(2026, 10)));

        // Act: DATETIME(3) não guarda fuso; o que está na coluna é um relógio de parede.
        LocalDateTime relogioDeParedeNoBanco = jdbc
                .sql("SELECT criado_em FROM registro_de_exemplo WHERE id = ?")
                .param(gravado.getId())
                .query(LocalDateTime.class)
                .single();

        // Assert: lido como UTC, esse relógio aponta o mesmo momento do criadoEm da entidade
        // (C5). O teste roda com o fuso do notebook (UTC-3), então um mapeamento que gravasse
        // a hora local erraria em 3 horas. A tolerância cobre o arredondamento do DATETIME(3).
        assertThat(relogioDeParedeNoBanco.toInstant(ZoneOffset.UTC))
                .isCloseTo(gravado.getCriadoEm(), within(1, ChronoUnit.MILLIS));
    }
}
