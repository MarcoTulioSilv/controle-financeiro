package com.marcotulio.financeiro.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Página em que o usuário logado troca a própria senha (RF06). */
@Transactional
class TrocaDeSenhaWebTest extends TesteDeIntegracao {

    private static final String LOGIN = "visitante";
    private static final String SENHA_ATUAL = "senha-atual-de-teste";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @BeforeEach
    void cadastraOUsuario() {
        destinatarios.cadastrarSeAusente("Visitante", "@vis");
        usuarios.cadastrarSeAusente("Visitante", LOGIN, SENHA_ATUAL, "@vis");
    }

    @Test
    void paginaExigeLogin() throws Exception {
        // Act e Assert
        mvc.perform(get("/conta/senha"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void mostraOFormularioAoUsuarioLogado() throws Exception {
        // Act e Assert
        mvc.perform(get("/conta/senha").with(user(LOGIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("name=\"novaSenha\"")));
    }

    @Test
    void trocaASenhaQuandoASenhaAtualConfere() throws Exception {
        // Act
        mvc.perform(trocarSenha(SENHA_ATUAL, "nova-senha-de-teste", "nova-senha-de-teste"))
                .andExpect(redirectedUrl("/conta/senha?trocada"));

        // Assert
        assertThat(senhaConfere("nova-senha-de-teste")).isTrue();
    }

    @Test
    void naoTrocaComASenhaAtualErrada() throws Exception {
        // Act
        mvc.perform(trocarSenha("senha-errada", "nova-senha-de-teste", "nova-senha-de-teste"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("A senha atual não confere.")));

        // Assert
        assertThat(senhaConfere(SENHA_ATUAL)).isTrue();
    }

    @Test
    void naoTrocaQuandoAConfirmacaoEhDiferente() throws Exception {
        // Act
        mvc.perform(trocarSenha(SENHA_ATUAL, "nova-senha-de-teste", "outra-senha-de-teste"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("A confirmação não confere")));

        // Assert
        assertThat(senhaConfere(SENHA_ATUAL)).isTrue();
    }

    @Test
    void naoTrocaPorSenhaComMenosDeOitoCaracteres() throws Exception {
        // Act
        mvc.perform(trocarSenha(SENHA_ATUAL, "1234567", "1234567"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("pelo menos 8 caracteres")));

        // Assert
        assertThat(senhaConfere(SENHA_ATUAL)).isTrue();
    }

    @Test
    void naoTrocaPorSenhaAcimaDoLimiteDoBcrypt() throws Exception {
        // Arrange: o BCrypt só aceita até 72 bytes; "ç" ocupa 2 bytes em UTF-8.
        String longaDemais = "ç".repeat(37);

        // Act
        mvc.perform(trocarSenha(SENHA_ATUAL, longaDemais, longaDemais))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("longa demais")));

        // Assert
        assertThat(senhaConfere(SENHA_ATUAL)).isTrue();
    }

    @Test
    void recusaATrocaSemTokenCsrf() throws Exception {
        // Act
        mvc.perform(post("/conta/senha").with(user(LOGIN))
                        .param("senhaAtual", SENHA_ATUAL)
                        .param("novaSenha", "nova-senha-de-teste")
                        .param("confirmacao", "nova-senha-de-teste"))
                .andExpect(status().isForbidden());

        // Assert
        assertThat(senhaConfere(SENHA_ATUAL)).isTrue();
    }

    private MockHttpServletRequestBuilder trocarSenha(
            String senhaAtual, String novaSenha, String confirmacao) {
        return post("/conta/senha").with(user(LOGIN)).with(csrf())
                .param("senhaAtual", senhaAtual)
                .param("novaSenha", novaSenha)
                .param("confirmacao", confirmacao);
    }

    private boolean senhaConfere(String senha) {
        String senhaHash = usuarios.buscarCredencial(LOGIN).orElseThrow().senhaHash();
        return codificadorDeSenha.matches(senha, senhaHash);
    }
}
