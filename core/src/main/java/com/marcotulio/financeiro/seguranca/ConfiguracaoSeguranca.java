package com.marcotulio.financeiro.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Configuração do Spring Security. */
@Configuration(proxyBeanMethods = false)
class ConfiguracaoSeguranca {

    /**
     * Gera hashes BCrypt (hash forte, RNF02) com o nome do algoritmo como prefixo, ex.:
     * {@code {bcrypt}$2a$10$...}. O prefixo permite trocar de algoritmo no futuro sem invalidar
     * as senhas já gravadas: cada hash diz como deve ser conferido.
     */
    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
