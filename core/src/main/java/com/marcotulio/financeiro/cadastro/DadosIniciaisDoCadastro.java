package com.marcotulio.financeiro.cadastro;

import java.util.List;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * A parte do arquivo {@code config/dados-iniciais.yml} que pertence ao cadastro. O formato está
 * documentado no exemplo versionado, {@code config/dados-iniciais.example.yml}.
 *
 * <p>Os nomes em YAML usam hífen ({@code marcacao-bot}) e o Spring os associa aos campos em
 * camelCase ({@code marcacaoBot}): é o "relaxed binding", o mesmo do {@code application.yaml}.
 */
record DadosIniciaisDoCadastro(
        @DefaultValue List<DestinatarioInicial> destinatarios,
        @DefaultValue List<CategoriaInicial> categorias) {

    record DestinatarioInicial(String nome, String marcacaoBot) {
    }

    /** {@code ativa} é opcional no arquivo e vale {@code true} quando omitido. */
    record CategoriaInicial(
            String nome,
            TipoCategoria tipo,
            @DefaultValue("true") boolean ativa,
            @DefaultValue List<String> palavrasChave) {
    }
}
