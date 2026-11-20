package com.marcotulio.financeiro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes que sobem o contexto do Spring com um banco real.
 *
 * <p>Como a configuração é a mesma para todas as subclasses, o Spring reaproveita o contexto
 * em cache e o container do MySQL é iniciado uma única vez na execução dos testes. Por isso os
 * recursos usados só pelos testes da camada web também ficam aqui:
 * <ul>
 *   <li>{@code RANDOM_PORT}: sobe o servidor de verdade (em {@code 127.0.0.1}, numa porta livre),
 *       para os testes que precisam ver a resposta HTTP real, como o cookie de sessão;</li>
 *   <li>{@code @AutoConfigureMockMvc}: simula requisições sem rede, com os filtros do Spring
 *       Security, para os demais testes web.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("teste")
@Import(ConfiguracaoMySqlDeTeste.class)
public abstract class TesteDeIntegracao {
}
