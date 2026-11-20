package com.marcotulio.financeiro.seguranca;

import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Página em que o usuário logado troca a própria senha (RF06). Só a própria: o login vem da
 * sessão, nunca do formulário.
 */
@Controller
@RequestMapping("/conta/senha")
class ControladorTrocaDeSenha {

    static final int TAMANHO_MINIMO = 8;

    /** O BCrypt só considera os primeiros 72 bytes da senha e recusa senhas maiores. */
    static final int LIMITE_DO_BCRYPT_EM_BYTES = 72;

    private static final String PAGINA = "conta/trocar-senha";

    private final ServicoUsuarios usuarios;

    ControladorTrocaDeSenha(ServicoUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    String formulario() {
        return PAGINA;
    }

    /**
     * Em caso de erro, a página volta com a mensagem e os campos vazios: senhas nunca são
     * devolvidas ao navegador. Em caso de sucesso, o redirecionamento evita que atualizar a
     * página reenvie o formulário.
     */
    @PostMapping
    String trocar(
            Principal usuarioLogado,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            @RequestParam String confirmacao,
            Model modelo) {
        String erro = conferirNovaSenha(novaSenha, confirmacao);
        if (erro == null && !usuarios.trocarSenha(usuarioLogado.getName(), senhaAtual, novaSenha)) {
            erro = "A senha atual não confere.";
        }
        if (erro != null) {
            modelo.addAttribute("erro", erro);
            return PAGINA;
        }
        return "redirect:/conta/senha?trocada";
    }

    /** Devolve a mensagem de erro, ou {@code null} se a nova senha pode ser usada. */
    private static String conferirNovaSenha(String novaSenha, String confirmacao) {
        if (novaSenha.length() < TAMANHO_MINIMO) {
            return "A senha nova precisa ter pelo menos " + TAMANHO_MINIMO + " caracteres.";
        }
        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > LIMITE_DO_BCRYPT_EM_BYTES) {
            return "A senha nova é longa demais: use até " + LIMITE_DO_BCRYPT_EM_BYTES
                    + " caracteres (letras acentuadas contam como dois).";
        }
        if (!novaSenha.equals(confirmacao)) {
            return "A confirmação não confere com a senha nova.";
        }
        return null;
    }
}
