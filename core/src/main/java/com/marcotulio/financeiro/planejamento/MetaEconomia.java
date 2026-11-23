package com.marcotulio.financeiro.planejamento;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Percentual da receita do mês que o domicílio quer economizar (RF04), a partir de um mês.
 * Mesma vigência do {@link Orcamento}: uma meta nova não altera os meses anteriores.
 */
@Entity
@Table(name = "meta_economia")
class MetaEconomia extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vigente_desde", nullable = false)
    private YearMonth vigenteDesde;

    /** De 0 a 100, com duas casas (ex.: 10.00 = 10%). Não é dinheiro, mas também é exato. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    protected MetaEconomia() {
        // exigido pelo JPA
    }

    MetaEconomia(YearMonth vigenteDesde, BigDecimal percentual) {
        this.vigenteDesde = vigenteDesde;
        this.percentual = percentual;
    }
}
