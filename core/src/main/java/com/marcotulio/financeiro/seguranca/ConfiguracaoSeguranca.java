package com.marcotulio.financeiro.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração do Spring Security: quem pode ver o quê, como se entra e se sai, e como as
 * senhas viram hash.
 *
 * <p>O que não aparece aqui também está ligado, porque é o padrão do Spring Security:
 * <ul>
 *   <li>CSRF: todo formulário que altera algo (POST) precisa levar um token secreto da sessão,
 *       que outro site não consegue ler. O Thymeleaf insere esse token nos formulários sozinho;</li>
 *   <li>troca do identificador da sessão ao entrar (contra "fixação de sessão");</li>
 *   <li>cabeçalhos de segurança nas respostas (ex.: proibir a página dentro de um iframe).</li>
 * </ul>
 * O cookie de sessão ({@code HttpOnly} e {@code SameSite}) é configurado no {@code application.yaml}.
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain filtrosDeSeguranca(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(pedidos -> pedidos
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("login")
                        .passwordParameter("senha")
                        .failureHandler(ConfiguracaoSeguranca::voltarAoLoginComOMotivo))
                .logout(logout -> logout.logoutSuccessUrl("/login?saiu"));
        return http.build();
    }

    /**
     * Gera hashes BCrypt (hash forte, RNF02) com o nome do algoritmo como prefixo, ex.:
     * {@code {bcrypt}$2a$10$...}. O prefixo permite trocar de algoritmo no futuro sem invalidar
     * as senhas já gravadas: cada hash diz como deve ser conferido.
     */
    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * O bloqueio tem mensagem própria, para quem errou a senha saber que precisa esperar. Os
     * demais motivos (login inexistente ou senha errada) recebem a mesma mensagem, para não
     * revelar quais logins existem.
     */
    private static void voltarAoLoginComOMotivo(
            HttpServletRequest pedido, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        String motivo = erro instanceof LockedException ? "bloqueado" : "erro";
        resposta.sendRedirect(pedido.getContextPath() + "/login?" + motivo);
    }
}
