package com.marcotulio.financeiro;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Sobe um MySQL 8.4 real em container para os testes, na mesma versão do docker-compose.yml.
 *
 * <p>A anotação {@code @ServiceConnection} faz o Spring Boot apontar o {@code DataSource} para
 * esse container: nenhum teste depende do MySQL instalado na máquina nem das variáveis do
 * {@code .env}, e por isso eles também rodam no pipeline.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracaoMySqlDeTeste {

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        return new MySQLContainer("mysql:8.4");
    }
}
