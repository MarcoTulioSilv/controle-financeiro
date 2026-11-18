package com.marcotulio.financeiro.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Página inicial, para onde o usuário vai depois de entrar. */
class InicioWebTest extends TesteDeIntegracao {

    @Autowired
    private MockMvc mvc;

    @Test
    void mostraQuemEstaConectadoEOBotaoDeSairComTokenCsrf() throws Exception {
        // Act e Assert: o formulário de sair leva o token CSRF, inserido pelo Thymeleaf.
        mvc.perform(get("/").with(user("visitante")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("visitante")))
                .andExpect(content().string(Matchers.containsString("action=\"/logout\"")))
                .andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));
    }
}
