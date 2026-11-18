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
        @DefaultValue List<CategoriaInicial> categorias,
        @DefaultValue List<UsuarioInicial> usuarios) {

    record DestinatarioInicial(String nome, String marcacaoBot) {
    }

    /**
     * {@code destinatario} é a marcação do destinatário que o usuário representa (ex.:
     * {@code @ana}). A senha inicial só vale até o primeiro acesso, quando o usuário a troca.
     */
    record UsuarioInicial(String nome, String login, String senhaInicial, String destinatario) {

        /** Esconde a senha: um erro de carga que imprima o registro não a expõe no log. */
        @Override
        public String toString() {
            return "UsuarioInicial[login=" + login + ", senhaInicial=***]";
        }
    }

    /** {@code ativa} é opcional no arquivo e vale {@code true} quando omitido. */
    record CategoriaInicial(
            String nome,
            TipoCategoria tipo,
            @DefaultValue("true") boolean ativa,
            @DefaultValue List<String> palavrasChave) {
    }
}
