package com.marcotulio.financeiro.cartao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Dias úteis bancários (RF16): usado para achar o vencimento efetivo de uma fatura (RF14, RN03).
 * Função pura: os feriados chegam por parâmetro, sem banco e sem relógio.
 */
public final class CalendarioBancario {

    /** A própria data, se for dia útil; senão, o primeiro dia útil depois dela. */
    public LocalDate proximoDiaUtil(LocalDate data, Set<LocalDate> feriados) {
        LocalDate dia = data;
        while (naoEhDiaUtil(dia, feriados)) {
            dia = dia.plusDays(1);
        }
        return dia;
    }

    private static boolean naoEhDiaUtil(LocalDate dia, Set<LocalDate> feriados) {
        DayOfWeek diaDaSemana = dia.getDayOfWeek();
        return diaDaSemana == DayOfWeek.SATURDAY
                || diaDaSemana == DayOfWeek.SUNDAY
                || feriados.contains(dia);
    }
}
