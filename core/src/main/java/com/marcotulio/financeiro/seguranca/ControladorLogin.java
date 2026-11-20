package com.marcotulio.financeiro.seguranca;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mostra a página de login. Quem recebe o formulário enviado (POST {@code /login}) e o
 * {@code /logout} é o próprio Spring Security, configurado em {@link ConfiguracaoSeguranca}.
 */
@Controller
class ControladorLogin {

    @GetMapping("/login")
    String paginaDeLogin() {
        return "login";
    }
}
