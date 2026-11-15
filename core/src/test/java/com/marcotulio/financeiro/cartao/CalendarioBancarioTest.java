package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarioBancarioTest {

    private final CalendarioBancario calendario = new CalendarioBancario();

    private final Set<LocalDate> semFeriados = Set.of();

    @Test
    void diaUtilDevolveAPropriaData() {
        // Arrange (preparar): quarta-feira, 07/10/2026
        LocalDate quarta = LocalDate.of(2026, 10, 7);

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(quarta, semFeriados);

        // Assert (verificar)
        assertThat(resultado).isEqualTo(quarta);
    }

    @Test
    void sabadoAvancaParaASegundaFeira() {
        // Arrange (preparar): sábado, 17/10/2026
        LocalDate sabado = LocalDate.of(2026, 10, 17);

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(sabado, semFeriados);

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void domingoAvancaParaASegundaFeira() {
        // Arrange (preparar): domingo, 18/10/2026
        LocalDate domingo = LocalDate.of(2026, 10, 18);

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(domingo, semFeriados);

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void feriadoEmDiaDeSemanaAvancaParaODiaSeguinte() {
        // Arrange (preparar): Finados, segunda-feira, 02/11/2026
        LocalDate finados = LocalDate.of(2026, 11, 2);

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(finados, Set.of(finados));

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void sabadoSeguidoDeFeriadoNaSegundaAvancaParaATerca() {
        // Arrange (preparar): sábado, 31/10/2026; a segunda, 02/11/2026, é Finados
        LocalDate sabado = LocalDate.of(2026, 10, 31);
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 11, 2));

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(sabado, feriados);

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void feriadosEmendadosAvancamAteOPrimeiroDiaUtil() {
        // Arrange (preparar): Carnaval de 2026, segunda 16/02 e terça 17/02, logo após o fim de semana
        LocalDate sabado = LocalDate.of(2026, 2, 14);
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17));

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(sabado, feriados);

        // Assert (verificar): quarta-feira de Cinzas, 18/02/2026
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 2, 18));
    }

    @Test
    void feriadoNaSextaSeguidoDeFimDeSemanaAvancaParaASegunda() {
        // Arrange (preparar): Sexta-feira Santa, 03/04/2026
        LocalDate sextaSanta = LocalDate.of(2026, 4, 3);

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(sextaSanta, Set.of(sextaSanta));

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 4, 6));
    }

    @Test
    void avancaPelaViradaDoAno() {
        // Arrange (preparar): sábado, 31/12/2022; domingo, 01/01/2023, é Confraternização Universal
        LocalDate sabado = LocalDate.of(2022, 12, 31);
        Set<LocalDate> feriados = Set.of(LocalDate.of(2023, 1, 1));

        // Act (agir)
        LocalDate resultado = calendario.proximoDiaUtil(sabado, feriados);

        // Assert (verificar)
        assertThat(resultado).isEqualTo(LocalDate.of(2023, 1, 2));
    }
}
