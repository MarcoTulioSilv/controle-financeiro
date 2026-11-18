package com.marcotulio.financeiro.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página inicial, para onde o usuário vai depois de entrar. As telas do sistema virão aqui. */
@Controller
class ControladorInicio {

    /** O {@link Principal} é o usuário logado, preenchido pelo Spring Security. */
    @GetMapping("/")
    String inicio(Principal usuarioLogado, Model modelo) {
        modelo.addAttribute("login", usuarioLogado.getName());
        return "inicio";
    }
}
