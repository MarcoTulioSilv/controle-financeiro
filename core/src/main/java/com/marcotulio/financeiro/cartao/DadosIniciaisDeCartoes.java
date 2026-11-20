package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * A parte do arquivo {@code config/dados-iniciais.yml} que pertence aos cartões. O formato está
 * documentado no exemplo versionado, {@code config/dados-iniciais.example.yml}.
 */
record DadosIniciaisDeCartoes(
        @DefaultValue List<CartaoInicial> cartoes,
        @DefaultValue List<FeriadoLocalInicial> feriadosLocais) {

    /** {@code titular} é o login do usuário titular do cartão (ex.: {@code ana}). */
    record CartaoInicial(
            String apelido,
            String nome,
            String titular,
            int diaFechamento,
            int diaVencimento,
            CompraNoFechamento compraNoFechamento) {

        RegraCartao regra() {
            return new RegraCartao(diaFechamento, diaVencimento, compraNoFechamento);
        }
    }

    record FeriadoLocalInicial(LocalDate data, String descricao) {
    }
}
