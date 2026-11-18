package com.marcotulio.financeiro.cadastro;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Palavra que leva o bot a deduzir a categoria de um lançamento (RF02). */
@Entity
@Table(name = "palavra_chave")
class PalavraChave extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String texto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    protected PalavraChave() {
        // exigido pelo JPA
    }

    PalavraChave(String texto, Categoria categoria) {
        this.texto = texto;
        this.categoria = categoria;
    }
}
