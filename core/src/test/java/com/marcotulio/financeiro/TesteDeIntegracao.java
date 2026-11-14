package com.marcotulio.financeiro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes que sobem o contexto do Spring com um banco real.
 *
 * <p>Como a configuração é a mesma para todas as subclasses, o Spring reaproveita o contexto
 * em cache e o container do MySQL é iniciado uma única vez na execução dos testes.
 */
@SpringBootTest
@ActiveProfiles("teste")
@Import(ConfiguracaoMySqlDeTeste.class)
public abstract class TesteDeIntegracao {
}
