package com.marcotulio.financeiro.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** Convenção C5: o mês de referência é gravado como DATE com dia 1. */
class ConversorMesReferenciaTest {

    private final ConversorMesReferencia conversor = new ConversorMesReferencia();

    @Test
    void gravaOMesDeReferenciaComoDataNoDiaUm() {
        // Arrange
        YearMonth outubroDe2026 = YearMonth.of(2026, 10);

        // Act
        LocalDate coluna = conversor.convertToDatabaseColumn(outubroDe2026);

        // Assert
        assertThat(coluna).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void leOMesDeReferenciaDeUmaDataDoBanco() {
        // Arrange
        LocalDate coluna = LocalDate.of(2027, 2, 1);

        // Act
        YearMonth mesReferencia = conversor.convertToEntityAttribute(coluna);

        // Assert
        assertThat(mesReferencia).isEqualTo(YearMonth.of(2027, 2));
    }

    @Test
    void mantemONuloNosDoisSentidos() {
        // Arrange, Act e Assert
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void ignoraODiaQuandoAColunaVemComOutroDia() {
        // Arrange: situação que o CHECK (DAY(col) = 1) impede no banco, mas pode
        // aparecer em dados importados por fora da aplicação.
        LocalDate colunaComDiaErrado = LocalDate.of(2026, 10, 17);

        // Act
        YearMonth mesReferencia = conversor.convertToEntityAttribute(colunaComDiaErrado);

        // Assert
        assertThat(mesReferencia).isEqualTo(YearMonth.of(2026, 10));
    }
}
