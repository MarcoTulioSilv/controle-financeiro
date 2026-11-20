package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública do calendário de feriados bancários (RF16). */
@Service
public class ServicoFeriados {

    private final RepositorioDeFeriados feriados;
    private final CalendarioBancario calendario = new CalendarioBancario();

    ServicoFeriados(RepositorioDeFeriados feriados) {
        this.feriados = feriados;
    }

    /**
     * Grava os feriados nacionais do {@code ano}, calculados pelo {@link CalendarioBancario}.
     * Idempotente: um dia que já está no banco (nacional ou local) não é gravado de novo.
     */
    @Transactional
    public void gravarFeriadosNacionais(int ano) {
        for (Map.Entry<LocalDate, String> feriado : calendario.feriadosNacionais(ano).entrySet()) {
            gravarSeAusente(feriado.getKey(), feriado.getValue(), AbrangenciaFeriado.NACIONAL);
        }
    }

    /** Cadastra um feriado da cidade, a menos que o dia já seja feriado. */
    @Transactional
    public void cadastrarLocalSeAusente(LocalDate data, String descricao) {
        gravarSeAusente(data, descricao, AbrangenciaFeriado.LOCAL);
    }

    /** Os feriados gravados entre as duas datas, inclusive. */
    @Transactional(readOnly = true)
    public Set<LocalDate> feriadosEntre(LocalDate inicio, LocalDate fim) {
        return feriados.datasEntre(inicio, fim);
    }

    private void gravarSeAusente(LocalDate data, String descricao, AbrangenciaFeriado abrangencia) {
        if (!feriados.existsByData(data)) {
            feriados.save(new Feriado(data, descricao, abrangencia));
        }
    }
}
