package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest(name = "Páscoa de {0}: {1}")
    @CsvSource({
        "2008, 2008-03-23", // Páscoa cedo
        "2011, 2011-04-24", // Páscoa tarde
        "2024, 2024-03-31",
        "2025, 2025-04-20",
        "2026, 2026-04-05",
        "2027, 2027-03-28",
        "2038, 2038-04-25", // a data mais tarde possível (25/04)
        "2285, 2285-03-22"  // a data mais cedo possível (22/03)
    })
    void calculaODomingoDePascoa(int ano, LocalDate pascoaEsperada) {
        // Arrange (preparar): o ano e a data conhecida vêm do @CsvSource

        // Act (agir)
        LocalDate pascoa = CalendarioBancario.domingoDePascoa(ano);

        // Assert (verificar)
        assertThat(pascoa).isEqualTo(pascoaEsperada);
    }

    // Os feriados esperados de 2026 e 2027 conferem com o calendário de feriados bancários
    // nacionais da Febraban e com as leis citadas em CalendarioBancario.feriadosNacionais.

    @Test
    void feriadosNacionaisDe2026ConferemComOCalendarioDaFebraban() {
        // Arrange (preparar): Páscoa em 05/04/2026
        int ano = 2026;

        // Act (agir)
        Map<LocalDate, String> feriados = calendario.feriadosNacionais(ano);

        // Assert (verificar)
        assertThat(feriados).containsOnly(
                entry(LocalDate.of(2026, 1, 1), "Confraternização Universal"),
                entry(LocalDate.of(2026, 2, 16), "Carnaval (segunda-feira)"),
                entry(LocalDate.of(2026, 2, 17), "Carnaval (terça-feira)"),
                entry(LocalDate.of(2026, 4, 3), "Sexta-feira Santa"),
                entry(LocalDate.of(2026, 4, 21), "Tiradentes"),
                entry(LocalDate.of(2026, 5, 1), "Dia do Trabalho"),
                entry(LocalDate.of(2026, 6, 4), "Corpus Christi"),
                entry(LocalDate.of(2026, 9, 7), "Independência do Brasil"),
                entry(LocalDate.of(2026, 10, 12), "Nossa Senhora Aparecida"),
                entry(LocalDate.of(2026, 11, 2), "Finados"),
                entry(LocalDate.of(2026, 11, 15), "Proclamação da República"),
                entry(LocalDate.of(2026, 11, 20), "Dia Nacional de Zumbi e da Consciência Negra"),
                entry(LocalDate.of(2026, 12, 25), "Natal"));
    }

    @Test
    void feriadosNacionaisDe2027ConferemComOCalendarioDaFebraban() {
        // Arrange (preparar): Páscoa em 28/03/2027, Carnaval ainda em fevereiro
        int ano = 2027;

        // Act (agir)
        Map<LocalDate, String> feriados = calendario.feriadosNacionais(ano);

        // Assert (verificar)
        assertThat(feriados).containsOnly(
                entry(LocalDate.of(2027, 1, 1), "Confraternização Universal"),
                entry(LocalDate.of(2027, 2, 8), "Carnaval (segunda-feira)"),
                entry(LocalDate.of(2027, 2, 9), "Carnaval (terça-feira)"),
                entry(LocalDate.of(2027, 3, 26), "Sexta-feira Santa"),
                entry(LocalDate.of(2027, 4, 21), "Tiradentes"),
                entry(LocalDate.of(2027, 5, 1), "Dia do Trabalho"),
                entry(LocalDate.of(2027, 5, 27), "Corpus Christi"),
                entry(LocalDate.of(2027, 9, 7), "Independência do Brasil"),
                entry(LocalDate.of(2027, 10, 12), "Nossa Senhora Aparecida"),
                entry(LocalDate.of(2027, 11, 2), "Finados"),
                entry(LocalDate.of(2027, 11, 15), "Proclamação da República"),
                entry(LocalDate.of(2027, 11, 20), "Dia Nacional de Zumbi e da Consciência Negra"),
                entry(LocalDate.of(2027, 12, 25), "Natal"));
    }

    @Test
    void conscienciaNegraSoEntraAPartirDe2024() {
        // Arrange (preparar): a Lei 14.759 é de 21/12/2023
        LocalDate vinteDeNovembroDe2023 = LocalDate.of(2023, 11, 20);
        LocalDate vinteDeNovembroDe2024 = LocalDate.of(2024, 11, 20);

        // Act (agir)
        Map<LocalDate, String> feriadosDe2023 = calendario.feriadosNacionais(2023);
        Map<LocalDate, String> feriadosDe2024 = calendario.feriadosNacionais(2024);

        // Assert (verificar)
        assertThat(feriadosDe2023).doesNotContainKey(vinteDeNovembroDe2023);
        assertThat(feriadosDe2024).containsKey(vinteDeNovembroDe2024);
    }

    @Test
    void feriadosNacionaisAlimentamOProximoDiaUtil() {
        // Arrange (preparar): vencimento nominal no Carnaval de 2027, segunda 08/02
        LocalDate vencimentoNominal = LocalDate.of(2027, 2, 8);

        // Act (agir)
        LocalDate vencimentoEfetivo = calendario.proximoDiaUtil(
                vencimentoNominal, calendario.feriadosNacionais(2027).keySet());

        // Assert (verificar): quarta-feira de Cinzas, 10/02/2027
        assertThat(vencimentoEfetivo).isEqualTo(LocalDate.of(2027, 2, 10));
    }
}
