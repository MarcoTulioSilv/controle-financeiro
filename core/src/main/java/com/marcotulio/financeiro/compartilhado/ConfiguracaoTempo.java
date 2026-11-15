package com.marcotulio.financeiro.compartilhado;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O tempo da aplicação.
 *
 * <p>Momentos são gravados em UTC (convenção C5, configurada em {@code application.yaml}), mas
 * as regras de negócio pensam no fuso do domicílio: a data da compra, o fechamento da fatura e
 * o mês de referência são os do calendário de São Paulo, não os do fuso do servidor.
 *
 * <p>O {@link Clock} é um bean para que os serviços recebam "o relógio" em vez de chamarem
 * {@code LocalDate.now()} direto: nos testes, basta injetar um relógio fixo.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoTempo {

    public static final ZoneId FUSO_DO_DOMICILIO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO_DO_DOMICILIO);
    }
}
