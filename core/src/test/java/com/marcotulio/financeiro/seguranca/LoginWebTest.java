package com.marcotulio.financeiro.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login por formulário, logout, CSRF, cookie de sessão e bloqueio temporário (RF06, RNF02).
 *
 * <p>O {@link MockMvc} roda na mesma thread do teste, então enxerga os usuários gravados dentro
 * da transação do teste, que é desfeita no final. O bloqueio, porém, fica na memória e dura o
 * contexto inteiro: por isso cada teste que erra senha usa um login próprio.
 */
@Transactional
class LoginWebTest extends TesteDeIntegracao {

    private static final String SENHA = "senha-de-teste";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @LocalServerPort
    private int porta;

    @BeforeEach
    void cadastraODestinatarioDosUsuarios() {
        destinatarios.cadastrarSeAusente("Visitante", "@vis");
    }

    @Test
    void paginaDeLoginEhPublica() throws Exception {
        // Act e Assert
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("name=\"senha\"")));
    }

    @Test
    void paginaProtegidaSemLoginLevaParaOLogin() throws Exception {
        // Act e Assert
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void entraComLoginESenhaCertos() throws Exception {
        // Arrange
        cadastrar("visitante-entra");

        // Act e Assert
        mvc.perform(entrar("visitante-entra", SENHA))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("visitante-entra"));
    }

    @Test
    void naoEntraComSenhaErrada() throws Exception {
        // Arrange
        cadastrar("visitante-erra");

        // Act e Assert
        mvc.perform(entrar("visitante-erra", "senha-errada"))
                .andExpect(redirectedUrl("/login?erro"))
                .andExpect(unauthenticated());
    }

    @Test
    void loginInexistenteRecebeAMesmaRespostaDeSenhaErrada() throws Exception {
        // Act e Assert: a resposta não revela quais logins existem.
        mvc.perform(entrar("ninguem", SENHA))
                .andExpect(redirectedUrl("/login?erro"))
                .andExpect(unauthenticated());
    }

    @Test
    void recusaOLoginSemTokenCsrf() throws Exception {
        // Arrange
        cadastrar("visitante-csrf");

        // Act e Assert: sem o token, um site malicioso poderia enviar o formulário pelo usuário.
        mvc.perform(post("/login").param("login", "visitante-csrf").param("senha", SENHA))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    void sairEncerraASessao() throws Exception {
        // Act e Assert
        mvc.perform(post("/logout").with(user("visitante")).with(csrf()))
                .andExpect(redirectedUrl("/login?saiu"))
                .andExpect(unauthenticated());
    }

    @Test
    void cincoSenhasErradasSeguidasBloqueiamAteASenhaCerta() throws Exception {
        // Arrange
        cadastrar("visitante-bloqueado");
        for (int i = 0; i < ControleDeTentativasDeLogin.MAXIMO_DE_FALHAS_SEGUIDAS; i++) {
            mvc.perform(entrar("visitante-bloqueado", "senha-errada"));
        }

        // Act e Assert
        mvc.perform(entrar("visitante-bloqueado", SENHA))
                .andExpect(redirectedUrl("/login?bloqueado"))
                .andExpect(unauthenticated());
    }

    @Test
    void cookieDeSessaoEhHttpOnlyESameSite() throws Exception {
        // Arrange: aqui é preciso o servidor de verdade, porque o MockMvc não gera cabeçalhos
        // de cookie. A página de login abre a sessão para guardar o token CSRF.
        URI paginaDeLogin = URI.create("http://127.0.0.1:" + porta + "/login");
        HttpRequest pedido = HttpRequest.newBuilder(paginaDeLogin).GET().build();

        // Act
        HttpResponse<Void> resposta;
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            resposta = cliente.send(pedido, HttpResponse.BodyHandlers.discarding());
        }

        // Assert
        String cookieDeSessao = resposta.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow();
        assertThat(cookieDeSessao).contains("HttpOnly").contains("SameSite=Strict");
    }

    private void cadastrar(String login) {
        usuarios.cadastrarSeAusente("Visitante", login, SENHA, "@vis");
    }

    /** Envia o formulário de login, com o token CSRF, como o navegador faria. */
    private static RequestBuilder entrar(String login, String senha) {
        return formLogin("/login")
                .userParameter("login")
                .passwordParam("senha")
                .user(login)
                .password(senha);
    }
}
