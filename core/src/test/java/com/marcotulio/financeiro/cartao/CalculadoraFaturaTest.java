package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraFaturaTest {

    private final CalculadoraFatura calculadora = new CalculadoraFatura();

    private final RegraCartao fecha25Vence5 =
            new RegraCartao(25, 5, CompraNoFechamento.PROXIMA);

    @Test
    void compraAntesDoFechamentoEntraNaFaturaQueVenceNoMesSeguinte() {
        // Arrange (preparar)
        LocalDate dataCompra = LocalDate.of(2026, 9, 24);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha25Vence5, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test 
    void compraDepoisDoFechamentoEntraNaFaturaSeguinte(){
        //Arrange(preparar)
        LocalDate dataCompra = LocalDate.of(2026, 9, 26);

        //Act(agir)
        FaturaCalculada fatura = calculadora.calcular(fecha25Vence5, dataCompra);

        //Assert(verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void compraNoDiaDoFechamentoComRegraAtualEntraNaFaturaQueEstaFechando() {
        // Arrange (preparar)
        RegraCartao fecha25Vence5Atual = new RegraCartao(25, 5, CompraNoFechamento.ATUAL);
        LocalDate dataCompra = LocalDate.of(2026, 9, 25);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha25Vence5Atual, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void compraNoDiaDoFechamentoComRegraProximaEntraNaFaturaSeguinte() {
        // Arrange (preparar)
        LocalDate dataCompra = LocalDate.of(2026, 9, 25);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha25Vence5, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void compraDepoisDoFechamentoDeDezembroCaiNaFaturaDoAnoSeguinte() {
        // Arrange (preparar)
        LocalDate dataCompra = LocalDate.of(2026, 12, 26);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha25Vence5, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2027, 1, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2027, 2, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2027, 2));
    }

    @Test
    void cartaoComVencimentoNoMesmoMesDoFechamentoReferenciaEsseMes() {
        // Arrange (preparar)
        RegraCartao fecha3Vence10 = new RegraCartao(3, 10, CompraNoFechamento.PROXIMA);
        LocalDate dataCompra = LocalDate.of(2026, 9, 21);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha3Vence10, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void diaDeFechamentoInexistenteNoMesUsaOUltimoDiaDoMes() {
        // Arrange (preparar)
        RegraCartao fecha30Vence5Atual = new RegraCartao(30, 5, CompraNoFechamento.ATUAL);
        LocalDate dataCompra = LocalDate.of(2027, 2, 28);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha30Vence5Atual, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2027, 3, 5));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2027, 3));
    }

    @Test
    void diaDeVencimentoInexistenteNoMesUsaOUltimoDiaDoMes() {
        // Arrange (preparar)
        RegraCartao fecha5Vence31 = new RegraCartao(5, 31, CompraNoFechamento.PROXIMA);
        LocalDate dataCompra = LocalDate.of(2027, 2, 2);

        // Act (agir)
        FaturaCalculada fatura = calculadora.calcular(fecha5Vence31, dataCompra);

        // Assert (verificar)
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2027, 2, 5));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2027, 2));
    }

    /**
     * O dia encurtado em um mês curto não pode "contaminar" os meses seguintes: cada ciclo
     * recomeça do dia configurado no cartão (31/01 → 28/02 → 31/03, e não 28/03).
     */
    @Test
    void cadaCicloRecomecaDoDiaConfiguradoENaoDoDiaEncurtado() {
        // Arrange (preparar)
        RegraCartao fecha31Vence10 = new RegraCartao(31, 10, CompraNoFechamento.PROXIMA);

        // Act (agir) — cada compra cai no dia do fechamento do seu mês, jogando para o ciclo seguinte
        FaturaCalculada aposFechamentoDeDezembro =
                calculadora.calcular(fecha31Vence10, LocalDate.of(2026, 12, 31));
        FaturaCalculada aposFechamentoDeJaneiro =
                calculadora.calcular(fecha31Vence10, LocalDate.of(2027, 1, 31));
        FaturaCalculada aposFechamentoDeFevereiro =
                calculadora.calcular(fecha31Vence10, LocalDate.of(2027, 2, 28));

        // Assert (verificar)
        assertThat(aposFechamentoDeDezembro.fechamento()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(aposFechamentoDeJaneiro.fechamento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(aposFechamentoDeFevereiro.fechamento()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(aposFechamentoDeFevereiro.vencimento()).isEqualTo(LocalDate.of(2027, 4, 10));
        assertThat(aposFechamentoDeFevereiro.mesReferencia()).isEqualTo(YearMonth.of(2027, 4));
    }

    // ----- fatura de um mês de referência (parcelas, RF12) -----

    @Test
    void faturaDoMesVenceNoDiaDoCartaoEFechaNoMesAnteriorQuandoOFechamentoVemDepoisDoVencimento() {
        // Act: cartão que fecha no dia 25 e vence no dia 5.
        FaturaCalculada fatura = calculadora.calcularPeloMes(fecha25Vence5, YearMonth.of(2026, 11));

        // Assert
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void faturaDoMesFechaNoProprioMesQuandoOFechamentoVemAntesDoVencimento() {
        // Arrange
        RegraCartao fecha3Vence10 = new RegraCartao(3, 10, CompraNoFechamento.PROXIMA);

        // Act
        FaturaCalculada fatura = calculadora.calcularPeloMes(fecha3Vence10, YearMonth.of(2026, 10));

        // Assert
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void faturaDoMesNaViradaDoAnoFechaEmDezembro() {
        // Act
        FaturaCalculada fatura = calculadora.calcularPeloMes(fecha25Vence5, YearMonth.of(2027, 1));

        // Assert
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    void faturaDoMesComDiasInexistentesUsaOUltimoDiaDoMes() {
        // Arrange
        RegraCartao fecha30Vence5 = new RegraCartao(30, 5, CompraNoFechamento.ATUAL);
        RegraCartao fecha5Vence31 = new RegraCartao(5, 31, CompraNoFechamento.PROXIMA);

        // Act
        FaturaCalculada fechaEmFevereiro = calculadora.calcularPeloMes(fecha30Vence5, YearMonth.of(2027, 3));
        FaturaCalculada venceEmFevereiro = calculadora.calcularPeloMes(fecha5Vence31, YearMonth.of(2027, 2));

        // Assert
        assertThat(fechaEmFevereiro.fechamento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(venceEmFevereiro.vencimento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(venceEmFevereiro.fechamento()).isEqualTo(LocalDate.of(2027, 2, 5));
    }

    @Test
    void faturaDoMesComFechamentoNoMesmoDiaDoVencimentoFechaUmMesAntes() {
        // Arrange: um dia não pode ser, ao mesmo tempo, o fechamento e o vencimento da fatura.
        RegraCartao fecha10Vence10 = new RegraCartao(10, 10, CompraNoFechamento.PROXIMA);

        // Act
        FaturaCalculada fatura = calculadora.calcularPeloMes(fecha10Vence10, YearMonth.of(2026, 10));

        // Assert
        assertThat(fatura.fechamento()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(fatura.vencimento()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    /**
     * As duas formas de chegar a uma fatura (pela data da compra e pelo mês de referência) têm de
     * concordar: a parcela 1 de uma compra parcelada entra pela data, e as seguintes pelo mês.
     * O teste confere todos os dias de dois anos nos cartões do ERS (Apêndice B) e nos dos casos
     * acima.
     */
    @Test
    void faturaDoMesCoincideComAFaturaCalculadaPelaDataDaCompra() {
        // Arrange
        List<RegraCartao> regras = List.of(
                new RegraCartao(20, 25, CompraNoFechamento.PROXIMA),
                new RegraCartao(10, 15, CompraNoFechamento.PROXIMA),
                new RegraCartao(5, 12, CompraNoFechamento.PROXIMA),
                new RegraCartao(15, 25, CompraNoFechamento.ATUAL),
                fecha25Vence5,
                new RegraCartao(3, 10, CompraNoFechamento.ATUAL),
                new RegraCartao(30, 5, CompraNoFechamento.ATUAL),
                new RegraCartao(31, 10, CompraNoFechamento.PROXIMA),
                new RegraCartao(5, 31, CompraNoFechamento.PROXIMA));

        for (RegraCartao regra : regras) {
            for (LocalDate dia = LocalDate.of(2026, 1, 1); dia.getYear() < 2028; dia = dia.plusDays(1)) {
                // Act
                FaturaCalculada pelaCompra = calculadora.calcular(regra, dia);
                FaturaCalculada peloMes = calculadora.calcularPeloMes(regra, pelaCompra.mesReferencia());

                // Assert
                assertThat(peloMes).as("%s, compra em %s", regra, dia).isEqualTo(pelaCompra);
            }
        }
    }
}
