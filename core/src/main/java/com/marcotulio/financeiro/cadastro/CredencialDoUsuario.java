package com.marcotulio.financeiro.cadastro;

/**
 * O que o módulo {@code seguranca} precisa para conferir um login: o login como está gravado e o
 * hash da senha. Nunca contém a senha em si.
 */
public record CredencialDoUsuario(String login, String senhaHash) {

    /** Esconde o hash: se um dia este objeto for parar num log, o hash não vai junto. */
    @Override
    public String toString() {
        return "CredencialDoUsuario[login=" + login + ", senhaHash=***]";
    }
}
