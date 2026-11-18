package com.marcotulio.financeiro.compartilhado;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Liga a auditoria do Spring Data JPA, que preenche as colunas da {@link EntidadeAuditavel}.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "autorDasAlteracoes")
class ConfiguracaoAuditoria {

    /** Autor das gravações feitas sem ninguém logado, como a carga de dados iniciais. */
    static final String AUTOR_SISTEMA = "sistema";

    /**
     * O autor é o login do usuário logado, que o Spring Security guarda no
     * {@link SecurityContextHolder} durante a requisição. Quem ainda não entrou é um visitante
     * "anônimo" e não vira autor.
     */
    @Bean
    AuditorAware<String> autorDasAlteracoes() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(autenticacao -> !(autenticacao instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName)
                .or(() -> Optional.of(AUTOR_SISTEMA));
    }
}
