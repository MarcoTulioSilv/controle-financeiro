package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
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
}