package com.marcotulio.financeiro.compartilhado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.YearMonth;

/**
 * Entidade usada somente nos testes, para exercitar o que o pacote {@code compartilhado}
 * oferece às entidades de verdade: as colunas de auditoria (C7) e o mês de referência (C5).
 * A tabela é criada por uma migração de teste, em {@code db/migracao-teste}.
 */
@Entity
@Table(name = "registro_de_exemplo")
class RegistroDeExemplo extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String descricao;

    @Column(name = "mes_referencia", nullable = false)
    private YearMonth mesReferencia;

    protected RegistroDeExemplo() {
        // exigido pelo JPA
    }

    RegistroDeExemplo(String descricao, YearMonth mesReferencia) {
        this.descricao = descricao;
        this.mesReferencia = mesReferencia;
    }

    Long getId() {
        return id;
    }

    String getDescricao() {
        return descricao;
    }

    void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    YearMonth getMesReferencia() {
        return mesReferencia;
    }
}
