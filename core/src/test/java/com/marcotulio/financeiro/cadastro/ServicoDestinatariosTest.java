package com.marcotulio.financeiro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de destinatários, o "para quem" dos lançamentos (RF13). */
@Transactional
class ServicoDestinatariosTest extends TesteDeIntegracao {

    @Autowired
    private ServicoDestinatarios servico;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void cadastraDestinatarioAtivo() {
        // Act
        DestinatarioCadastrado visitante = servico.cadastrarSeAusente("Visitante", "@vis");

        // Assert
        assertThat(visitante.id()).isPositive();
        assertThat(visitante.nome()).isEqualTo("Visitante");
        assertThat(visitante.marcacaoBot()).isEqualTo("@vis");
        assertThat(visitante.ativo()).isTrue();
    }

    @Test
    void naoDuplicaDestinatarioComAMesmaMarcacao() {
        // Arrange
        DestinatarioCadastrado primeiro = servico.cadastrarSeAusente("Visitante", "@vis");

        // Act
        DestinatarioCadastrado segundo = servico.cadastrarSeAusente("Visitante", "@vis");

        // Assert
        assertThat(segundo.id()).isEqualTo(primeiro.id());
        assertThat(contarComMarcacao("@vis")).isEqualTo(1);
    }

    @Test
    void recusaNomeJaUsadoPorOutraMarcacao() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "@vis");

        // Act e Assert: o UNIQUE do nome falha alto, em vez de criar um destinatário ambíguo.
        assertThatThrownBy(() -> servico.cadastrarSeAusente("Visitante", "@outro"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reconheceMarcacaoCadastradaSemDiferenciarMaiusculas() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "@vis");

        // Act e Assert
        assertThat(servico.existeMarcacao("@VIS")).isTrue();
        assertThat(servico.existeMarcacao("@outro")).isFalse();
    }

    private int contarComMarcacao(String marcacaoBot) {
        return jdbc.sql("SELECT COUNT(*) FROM destinatario WHERE marcacao_bot = ?")
                .param(marcacaoBot)
                .query(Integer.class)
                .single();
    }
}
