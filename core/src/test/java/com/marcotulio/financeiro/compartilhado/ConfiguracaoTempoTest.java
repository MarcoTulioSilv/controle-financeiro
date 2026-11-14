package com.marcotulio.financeiro.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** As regras de negócio raciocinam no fuso do domicílio, não no fuso do servidor. */
class ConfiguracaoTempoTest {

    @Test
    void oRelogioDaAplicacaoUsaOFusoDeSaoPaulo() {
        // Arrange
        ConfiguracaoTempo configuracao = new ConfiguracaoTempo();

        // Act
        ZoneId fuso = configuracao.relogio().getZone();

        // Assert
        assertThat(fuso).isEqualTo(ZoneId.of("America/Sao_Paulo"));
        assertThat(ConfiguracaoTempo.FUSO_DO_DOMICILIO).isEqualTo(fuso);
    }
}
