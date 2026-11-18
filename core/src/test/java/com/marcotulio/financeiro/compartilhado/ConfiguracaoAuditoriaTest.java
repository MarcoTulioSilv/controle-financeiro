package com.marcotulio.financeiro.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quem aparece como autor nas colunas de auditoria (C7). */
class ConfiguracaoAuditoriaTest {

    private final AuditorAware<String> autor = new ConfiguracaoAuditoria().autorDasAlteracoes();

    @AfterEach
    void limpaOUsuarioLogado() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void comUsuarioLogadoOAutorEhOLogin() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana", null, List.of()));

        // Act e Assert
        assertThat(autor.getCurrentAuditor()).contains("ana");
    }

    @Test
    void semUsuarioLogadoOAutorEhOSistema() {
        // Act e Assert: é o caso da carga de dados iniciais, que roda na subida da aplicação.
        assertThat(autor.getCurrentAuditor()).contains("sistema");
    }

    @Test
    void visitanteAnonimoNaoViraAutor() {
        // Arrange: o Spring Security chama de "anonymousUser" quem ainda não entrou.
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        // Act e Assert
        assertThat(autor.getCurrentAuditor()).contains("sistema");
    }
}
