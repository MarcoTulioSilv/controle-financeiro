package com.marcotulio.financeiro.cartao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
     * Os feriados sem expediente bancário em todo o país no {@code ano}, com a descrição
     * de cada um, em ordem de data.
     *
     * <p>Fontes: Lei 662/1949 (com a redação da Lei 10.607/2002), Lei 6.802/1980 (Nossa Senhora
     * Aparecida), Lei 14.759/2023 (Consciência Negra, desde 2024), Lei 9.093/1995 (Sexta-feira
     * Santa) e o calendário de feriados bancários nacionais da Febraban, que também fecha os
     * bancos na segunda e na terça de Carnaval e em Corpus Christi.
     */
    public Map<LocalDate, String> feriadosNacionais(int ano) {
        Map<LocalDate, String> feriados = new TreeMap<>();

        feriados.put(LocalDate.of(ano, 1, 1), "Confraternização Universal");
        feriados.put(LocalDate.of(ano, 4, 21), "Tiradentes");
        feriados.put(LocalDate.of(ano, 5, 1), "Dia do Trabalho");
        feriados.put(LocalDate.of(ano, 9, 7), "Independência do Brasil");
        feriados.put(LocalDate.of(ano, 10, 12), "Nossa Senhora Aparecida");
        feriados.put(LocalDate.of(ano, 11, 2), "Finados");
        feriados.put(LocalDate.of(ano, 11, 15), "Proclamação da República");
        if (ano >= 2024) {
            feriados.put(LocalDate.of(ano, 11, 20), "Dia Nacional de Zumbi e da Consciência Negra");
        }
        feriados.put(LocalDate.of(ano, 12, 25), "Natal");

        // Os feriados móveis são contados a partir do domingo de Páscoa.
        LocalDate pascoa = domingoDePascoa(ano);
        feriados.put(pascoa.minusDays(48), "Carnaval (segunda-feira)");
        feriados.put(pascoa.minusDays(47), "Carnaval (terça-feira)");
        feriados.put(pascoa.minusDays(2), "Sexta-feira Santa");
        feriados.put(pascoa.plusDays(60), "Corpus Christi");

        return Collections.unmodifiableMap(feriados);
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
