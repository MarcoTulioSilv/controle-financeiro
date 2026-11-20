package com.marcotulio.financeiro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de usuários e de suas senhas (RF06, RNF02). */
@Transactional
class ServicoUsuariosTest extends TesteDeIntegracao {

    private static final String SENHA_INICIAL = "senha-inicial-de-teste";

    @Autowired
    private ServicoUsuarios servico;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cadastraODestinatarioDoUsuario() {
        destinatarios.cadastrarSeAusente("Visitante", "@vis");
    }

    @Test
    void gravaSoOHashDaSenhaInicialNuncaOTexto() {
        // Act
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");

        // Assert
        String senhaGravada = senhaHashDe("visitante");
        assertThat(senhaGravada).doesNotContain(SENHA_INICIAL);
        // O prefixo diz qual algoritmo gerou o hash: BCrypt, hash forte do RNF02.
        assertThat(senhaGravada).startsWith("{bcrypt}");
        assertThat(codificadorDeSenha.matches(SENHA_INICIAL, senhaGravada)).isTrue();
    }

    @Test
    void ligaOUsuarioAoDestinatarioDaMarcacao() {
        // Act
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");

        // Assert
        String marcacao = jdbc.sql("""
                        SELECT d.marcacao_bot FROM usuario u
                          JOIN destinatario d ON d.id = u.destinatario_id
                         WHERE u.login = 'visitante'
                        """)
                .query(String.class)
                .single();
        assertThat(marcacao).isEqualTo("@vis");
    }

    @Test
    void naoDuplicaNemAlteraUsuarioJaCadastrado() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");
        String hashOriginal = senhaHashDe("visitante");

        // Act: rodar a carga de novo não pode desfazer uma senha trocada pelo usuário.
        servico.cadastrarSeAusente("Visitante", "visitante", "outra-senha-qualquer", "@vis");

        // Assert
        Integer total = jdbc.sql("SELECT COUNT(*) FROM usuario WHERE login = 'visitante'")
                .query(Integer.class)
                .single();
        assertThat(total).isEqualTo(1);
        assertThat(senhaHashDe("visitante")).isEqualTo(hashOriginal);
    }

    @Test
    void recusaMarcacaoDeDestinatarioInexistente() {
        // Act e Assert
        assertThatThrownBy(() -> servico.cadastrarSeAusente(
                        "Visitante", "visitante", SENHA_INICIAL, "@ninguem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@ninguem");
    }

    @Test
    void recusaUsuarioSemSenhaInicial() {
        // Act e Assert
        assertThatThrownBy(() -> servico.cadastrarSeAusente("Visitante", "visitante", null, "@vis"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encontraACredencialSemDiferenciarMaiusculas() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");

        // Act
        var credencial = servico.buscarCredencial("VISITANTE");

        // Assert: o login devolvido é o gravado, não o digitado.
        assertThat(credencial).hasValueSatisfying(encontrada -> {
            assertThat(encontrada.login()).isEqualTo("visitante");
            assertThat(codificadorDeSenha.matches(SENHA_INICIAL, encontrada.senhaHash())).isTrue();
        });
    }

    @Test
    void naoEncontraCredencialDeLoginInexistente() {
        // Act e Assert
        assertThat(servico.buscarCredencial("ninguem")).isEmpty();
    }

    @Test
    void encontraOIdDoUsuarioPeloLogin() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");
        long idGravado = jdbc.sql("SELECT id FROM usuario WHERE login = 'visitante'")
                .query(Long.class)
                .single();

        // Act e Assert
        assertThat(servico.buscarIdPeloLogin("Visitante")).hasValue(idGravado);
        assertThat(servico.buscarIdPeloLogin("ninguem")).isEmpty();
    }

    @Test
    void trocaASenhaQuandoASenhaAtualConfere() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");

        // Act
        boolean trocou = servico.trocarSenha("visitante", SENHA_INICIAL, "nova-senha-de-teste");

        // Assert
        assertThat(trocou).isTrue();
        assertThat(codificadorDeSenha.matches("nova-senha-de-teste", senhaHashDe("visitante")))
                .isTrue();
    }

    @Test
    void naoTrocaASenhaQuandoASenhaAtualNaoConfere() {
        // Arrange
        servico.cadastrarSeAusente("Visitante", "visitante", SENHA_INICIAL, "@vis");
        String hashOriginal = senhaHashDe("visitante");

        // Act
        boolean trocou = servico.trocarSenha("visitante", "senha-errada", "nova-senha-de-teste");

        // Assert
        assertThat(trocou).isFalse();
        assertThat(senhaHashDe("visitante")).isEqualTo(hashOriginal);
    }

    private String senhaHashDe(String login) {
        // O JDBC lê o banco por fora do Hibernate: sem o flush, a alteração feita na
        // entidade ainda não virou UPDATE (o teste é @Transactional e nunca chega ao commit).
        entityManager.flush();
        return jdbc.sql("SELECT senha_hash FROM usuario WHERE login = ?")
                .param(login)
                .query(String.class)
                .single();
    }
}
