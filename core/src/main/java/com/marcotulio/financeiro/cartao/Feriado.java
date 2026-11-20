package com.marcotulio.financeiro.cartao;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Dia sem expediente bancário, além de sábados e domingos (RF16). */
@Entity
@Table(name = "feriado")
class Feriado extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AbrangenciaFeriado abrangencia;

    protected Feriado() {
        // exigido pelo JPA
    }

    Feriado(LocalDate data, String descricao, AbrangenciaFeriado abrangencia) {
        this.data = data;
        this.descricao = descricao;
        this.abrangencia = abrangencia;
    }
}
