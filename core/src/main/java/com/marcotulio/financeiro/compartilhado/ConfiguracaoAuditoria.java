package com.marcotulio.financeiro.compartilhado;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Liga a auditoria do Spring Data JPA, que preenche as colunas da {@link EntidadeAuditavel}.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "autorDasAlteracoes")
class ConfiguracaoAuditoria {

    /** Autor usado enquanto não existe login (T08): toda gravação é do próprio sistema. */
    static final String AUTOR_SISTEMA = "sistema";

    @Bean
    AuditorAware<String> autorDasAlteracoes() {
        return () -> Optional.of(AUTOR_SISTEMA);
    }
}
