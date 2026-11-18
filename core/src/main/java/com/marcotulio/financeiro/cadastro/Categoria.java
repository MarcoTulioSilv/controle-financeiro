package com.marcotulio.financeiro.cadastro;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Categoria de receita ou de despesa (RF02).
 *
 * <p>Não há como excluir uma categoria: ela é desativada e continua nos lançamentos antigos e
 * nos relatórios. Por isso a entidade oferece {@link #desativar()}, e o repositório não declara
 * nenhum método de exclusão.
 */
@Entity
@Table(name = "categoria")
class Categoria extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nome;

    /** Gravado pelo nome ("RECEITA"), nunca pela posição no enum (convenção C6). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCategoria tipo;

    @Column(nullable = false)
    private boolean ativa = true;

    protected Categoria() {
        // exigido pelo JPA
    }

    Categoria(String nome, TipoCategoria tipo, boolean ativa) {
        this.nome = nome;
        this.tipo = tipo;
        this.ativa = ativa;
    }

    Long getId() {
        return id;
    }

    void desativar() {
        this.ativa = false;
    }

    CategoriaCadastrada paraConsulta() {
        return new CategoriaCadastrada(id, nome, tipo, ativa);
    }
}
