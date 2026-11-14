package com.marcotulio.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Confere as decisões de persistência da T01 dentro do contexto que os testes realmente usam.
 * Sem isso, um arquivo de configuração de teste mal colocado poderia desligar essas garantias
 * sem ninguém perceber.
 */
class ConfiguracaoPersistenciaTest extends TesteDeIntegracao {

    @Autowired
    private Environment ambiente;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void oSchemaVemSoDoFlyway() {
        // Arrange, Act e Assert
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void naoMantemASessaoAbertaAteARenderizacaoDaTela() {
        // Arrange, Act e Assert
        assertThat(ambiente.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void hibernateGravaMomentosEmUtc() {
        // Arrange, Act e Assert
        assertThat(ambiente.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"))
                .isEqualTo("UTC");
    }

    @Test
    void oFlywayRegistraOHistoricoDasMigracoes() {
        // Arrange: tabela que o Flyway cria sozinho ao aplicar as migrações.
        // Act
        Integer tabelas = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'
                        """)
                .query(Integer.class)
                .single();

        // Assert
        assertThat(tabelas).isEqualTo(1);
    }
}
