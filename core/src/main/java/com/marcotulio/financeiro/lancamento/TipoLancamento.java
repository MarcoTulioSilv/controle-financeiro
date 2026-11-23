package com.marcotulio.financeiro.lancamento;

import com.marcotulio.financeiro.cadastro.TipoCategoria;

/** Um lançamento é uma receita ou uma despesa. O valor é sempre positivo: o sentido vem daqui (C4). */
public enum TipoLancamento {
    RECEITA,
    DESPESA;

    /**
     * Uma receita só pode ser classificada por uma categoria de receita, e uma despesa por uma de
     * despesa (RF02). O {@code switch} sem {@code default} obriga a rever este método se um tipo
     * novo for criado: o compilador acusa o caso que faltar.
     */
    boolean combinaCom(TipoCategoria tipoCategoria) {
        return switch (this) {
            case RECEITA -> tipoCategoria == TipoCategoria.RECEITA;
            case DESPESA -> tipoCategoria == TipoCategoria.DESPESA;
        };
    }
}
