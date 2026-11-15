package com.marcotulio.financeiro.compartilhado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Converte o mês de referência entre {@link YearMonth} (Java) e {@code DATE} com dia 1 (MySQL),
 * como manda a convenção C5 do modelo de dados. O MySQL não tem um tipo "ano e mês"; usar
 * {@code DATE} permite ordenar e usar funções de data nas visões do Power BI.
 *
 * <p>{@code autoApply = true}: todo atributo {@code YearMonth} das entidades usa este conversor
 * sem precisar de {@code @Convert} em cada campo.
 */
@Converter(autoApply = true)
public class ConversorMesReferencia implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth mesReferencia) {
        return mesReferencia == null ? null : mesReferencia.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate coluna) {
        return coluna == null ? null : YearMonth.from(coluna);
    }
}
