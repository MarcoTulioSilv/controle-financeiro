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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cartão de crédito do domicílio (RF14).
 *
 * <p>O titular é guardado só pelo id: a entidade {@code Usuario} pertence ao módulo
 * {@code cadastro} e não é visível aqui (M5). O titular não decide o "para quem" das compras:
 * um cartão do Marco pode pagar um gasto da casa.
 */
@Entity
@Table(name = "cartao")
class Cartao extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A palavra que identifica o cartão na mensagem do bot, ex.: {@code bmt}. */
    @Column(nullable = false, length = 20)
    private String apelido;

    @Column(nullable = false, length = 50)
    private String nome;

    @Column(name = "titular_id", nullable = false)
    private Long titularId;

    // A coluna é TINYINT (um dia cabe em 1 byte); o @JdbcTypeCode avisa o Hibernate, que
    // esperaria INTEGER para um int e recusaria o schema na validação (ddl-auto=validate).
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "dia_fechamento", nullable = false)
    private int diaFechamento;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "compra_no_fechamento", nullable = false, length = 10)
    private CompraNoFechamento compraNoFechamento;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Cartao() {
        // exigido pelo JPA
    }

    Cartao(String apelido, String nome, long titularId, RegraCartao regra) {
        this.apelido = apelido;
        this.nome = nome;
        this.titularId = titularId;
        this.diaFechamento = regra.diaFechamento();
        this.diaVencimento = regra.diaVencimento();
        this.compraNoFechamento = regra.compraNoFechamento();
    }

    Long getId() {
        return id;
    }

    /** As regras de faturamento, no formato que a {@link CalculadoraFatura} entende. */
    RegraCartao regra() {
        return new RegraCartao(diaFechamento, diaVencimento, compraNoFechamento);
    }

    CartaoCadastrado paraConsulta() {
        return new CartaoCadastrado(id, apelido, nome, titularId, regra(), ativo);
    }
}
