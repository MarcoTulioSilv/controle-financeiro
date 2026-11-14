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
}