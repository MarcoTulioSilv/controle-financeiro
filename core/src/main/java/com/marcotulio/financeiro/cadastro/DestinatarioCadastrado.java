package com.marcotulio.financeiro.cadastro;

/**
 * O que os outros módulos enxergam de um destinatário: uma cópia imutável dos dados, sem acesso
 * à entidade JPA (que só o pacote {@code cadastro} pode alterar).
 */
public record DestinatarioCadastrado(long id, String nome, String marcacaoBot, boolean ativo) {
}
