package com.marcotulio.financeiro.lancamento;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma compra parcelada, um financiamento ou um empréstimo (RF12). As parcelas são lançamentos
 * comuns, ligados a ela por {@code divida_id} e {@code numero_parcela}.
 *
 * <p>Não há campo de saldo devedor: ele é calculado a partir das parcelas
 * ({@link ServicoDividas#saldoDevedor}), para que não existam duas fontes da mesma informação.
 *
 * <p>Fica de fora do mapeamento, porque só é usada depois do MVP 1: {@code data_quitacao}
 * (a quitação). O banco a grava nula.
 */
@Entity
@Table(name = "divida")
class Divida extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDivida tipo;

    /** O item, sem o número da parcela (ex.: "celular"). */
    @Column(nullable = false, length = 200)
    private String descricao;

    /** A soma exata das parcelas, como foram geradas (ADR-005). */
    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    // A coluna é SMALLINT: sem o @JdbcTypeCode, o Hibernate esperaria INTEGER para um int e
    // recusaria o schema na validação (ddl-auto=validate), como no dia do Cartao.
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "quantidade_parcelas", nullable = false)
    private int quantidadeParcelas;

    @Column(name = "cartao_id")
    private Long cartaoId;

    /** O "para quem" (RF13) de todas as parcelas. */
    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(name = "data_contratacao", nullable = false)
    private LocalDate dataContratacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusDivida status;

    protected Divida() {
        // exigido pelo JPA
    }

    /**
     * Uma dívida nova nasce ativa.
     *
     * @param descricao já sem espaços nas pontas
     * @param valorTotal a soma das parcelas, com escala 2
     */
    Divida(DadosDaDivida dados, String descricao, BigDecimal valorTotal) {
        this.tipo = dados.tipo();
        this.descricao = descricao;
        this.valorTotal = valorTotal;
        this.quantidadeParcelas = dados.quantidadeParcelas();
        this.cartaoId = dados.cartaoId();
        this.destinatarioId = dados.destinatarioId();
        this.dataContratacao = dados.dataContratacao();
        this.status = StatusDivida.ATIVA;
    }

    long id() {
        return id;
    }

    DividaRegistrada paraConsulta(List<LancamentoRegistrado> parcelas) {
        return new DividaRegistrada(id, tipo, descricao, valorTotal, quantidadeParcelas, cartaoId,
                destinatarioId, dataContratacao, status, parcelas);
    }
}
