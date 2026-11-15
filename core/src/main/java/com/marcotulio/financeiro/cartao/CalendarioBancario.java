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

    /**
     * O domingo de Páscoa no calendário gregoriano, pelo algoritmo de Meeus/Jones/Butcher.
     * A Páscoa é o primeiro domingo depois da primeira lua cheia a partir de 21 de março; as
     * contas abaixo reproduzem essa regra com aritmética inteira, sem tabela de datas.
     */
    static LocalDate domingoDePascoa(int ano) {
        int a = ano % 19;                 // posição do ano no ciclo lunar de 19 anos (ciclo de Metão)
        int b = ano / 100;                // século
        int c = ano % 100;                // ano dentro do século
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;   // dias de 21/03 até a lua cheia pascal
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7; // dias da lua cheia até o domingo seguinte
        int m = (a + 11 * h + 22 * l) / 451;      // correção de casos raros (Páscoa após 25/04)
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    private static boolean naoEhDiaUtil(LocalDate dia, Set<LocalDate> feriados) {
        DayOfWeek diaDaSemana = dia.getDayOfWeek();
        return diaDaSemana == DayOfWeek.SATURDAY
                || diaDaSemana == DayOfWeek.SUNDAY
                || feriados.contains(dia);
    }
}
