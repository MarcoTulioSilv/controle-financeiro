package com.marcotulio.financeiro.cadastro;

/** O que os outros módulos enxergam de uma categoria: uma cópia imutável dos dados. */
public record CategoriaCadastrada(long id, String nome, TipoCategoria tipo, boolean ativa) {
}
