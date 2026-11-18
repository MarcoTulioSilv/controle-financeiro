package com.marcotulio.financeiro.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bloqueio temporário do login após tentativas seguidas de senha errada (RNF02). */
class ControleDeTentativasDeLoginTest {

    private RelogioDeTeste relogio;
    private ControleDeTentativasDeLogin controle;

    @BeforeEach
    void preparaOControleComUmRelogioParado() {
        relogio = new RelogioDeTeste(Instant.parse("2026-11-18T12:00:00Z"));
        controle = new ControleDeTentativasDeLogin(relogio);
    }

    @Test
    void loginSemFalhasNaoEstaBloqueado() {
        // Act e Assert
        assertThat(controle.estaBloqueado("ana")).isFalse();
    }

    @Test
    void quatroFalhasSeguidasAindaNaoBloqueiam() {
        // Act
        errarSenha("ana", 4);

        // Assert
        assertThat(controle.estaBloqueado("ana")).isFalse();
    }

    @Test
    void cincoFalhasSeguidasBloqueiam() {
        // Act
        errarSenha("ana", 5);

        // Assert
        assertThat(controle.estaBloqueado("ana")).isTrue();
    }

    @Test
    void acertarASenhaZeraAsFalhasAnteriores() {
        // Arrange
        errarSenha("ana", 4);
        controle.registrarSucesso("ana");

        // Act: só as falhas *seguidas* contam.
        errarSenha("ana", 4);

        // Assert
        assertThat(controle.estaBloqueado("ana")).isFalse();
    }

    @Test
    void oBloqueioDuraQuinzeMinutos() {
        // Arrange
        errarSenha("ana", 5);

        // Act
        relogio.avancar(Duration.ofMinutes(15).minusSeconds(1));

        // Assert
        assertThat(controle.estaBloqueado("ana")).isTrue();
    }

    @Test
    void depoisDeQuinzeMinutosOLoginEstaLiberado() {
        // Arrange
        errarSenha("ana", 5);

        // Act
        relogio.avancar(Duration.ofMinutes(15));

        // Assert
        assertThat(controle.estaBloqueado("ana")).isFalse();
    }

    @Test
    void depoisQueOBloqueioExpiraAContagemRecomecaDoZero() {
        // Arrange
        errarSenha("ana", 5);
        relogio.avancar(Duration.ofMinutes(15));

        // Act
        errarSenha("ana", 1);

        // Assert: um único erro depois do bloqueio não bloqueia de novo.
        assertThat(controle.estaBloqueado("ana")).isFalse();
    }

    @Test
    void maiusculasEAcentosNaoDriblamOBloqueio() {
        // Arrange: para o banco (collation ai_ci), todas estas formas são o mesmo login.
        controle.registrarFalha("ana");
        controle.registrarFalha("ANA");
        controle.registrarFalha("Ána");
        controle.registrarFalha("aNa");

        // Act
        controle.registrarFalha("ãna");

        // Assert
        assertThat(controle.estaBloqueado("ana")).isTrue();
    }

    @Test
    void oBloqueioDeUmLoginNaoAfetaOutro() {
        // Act
        errarSenha("ana", 5);

        // Assert
        assertThat(controle.estaBloqueado("marco")).isFalse();
    }

    private void errarSenha(String login, int vezes) {
        for (int i = 0; i < vezes; i++) {
            controle.registrarFalha(login);
        }
    }
}
