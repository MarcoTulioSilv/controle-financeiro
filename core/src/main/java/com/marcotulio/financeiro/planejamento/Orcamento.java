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
 * O valor planejado de uma categoria a partir de um mês (RF04).
 *
 * <p>Dado com vigência: mudar o orçamento é gravar um {@code Orcamento} novo, com outro
 * {@code vigenteDesde}; o antigo continua valendo para os meses anteriores. A categoria pertence
 * ao cadastro e é guardada só pelo id (M5).
 */
@Entity
@Table(name = "orcamento")
class Orcamento extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    /** Gravado como DATE com dia 1 pelo {@code ConversorMesReferencia} (C5). */
    @Column(name = "vigente_desde", nullable = false)
    private YearMonth vigenteDesde;

    @Column(name = "valor_planejado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPlanejado;

    protected Orcamento() {
        // exigido pelo JPA
    }

    Orcamento(long categoriaId, YearMonth vigenteDesde, BigDecimal valorPlanejado) {
        this.categoriaId = categoriaId;
        this.vigenteDesde = vigenteDesde;
        this.valorPlanejado = valorPlanejado;
    }
}
