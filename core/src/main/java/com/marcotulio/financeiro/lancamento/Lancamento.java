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
import java.time.YearMonth;

/**
 * Uma receita ou uma despesa (RF01).
 *
 * <p>Categoria, destinatário, cartão e fatura pertencem a outros módulos e são guardados só pelo
 * id (M5). A entidade só aceita dados já conferidos ({@link LancamentoConferido}): não há como
 * gravar um lançamento que pulou as {@link RegrasDoLancamento}.
 *
 * <p>Colunas da tabela que ficam de fora do mapeamento, porque só são usadas depois do MVP 1:
 * {@code mensagem_original} (bot), {@code revisar} (revisão de fatura, RF15) e
 * {@code excluido_em} (exclusão lógica com validação cruzada, RF10). O banco grava os valores
 * padrão delas.
 */
@Entity
@Table(name = "lancamento")
class Lancamento extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    /** O "para quem" (RF13). */
    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    /** Gravado como DATE com dia 1 pelo {@code ConversorMesReferencia} (C5). */
    @Column(name = "mes_referencia", nullable = false)
    private YearMonth mesReferencia;

    /** Definida no registro; a edição não a altera. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private OrigemLancamento origem;

    @Column(name = "cartao_id")
    private Long cartaoId;

    @Column(name = "fatura_id")
    private Long faturaId;

    protected Lancamento() {
        // exigido pelo JPA
    }

    Lancamento(LancamentoConferido conferido, OrigemLancamento origem) {
        this.origem = origem;
        alterar(conferido);
    }

    /** A edição troca todos os dados informados, já conferidos de novo. */
    final void alterar(LancamentoConferido conferido) {
        this.tipo = conferido.tipo();
        this.valor = conferido.valor();
        this.descricao = conferido.descricao();
        this.categoriaId = conferido.categoriaId();
        this.destinatarioId = conferido.destinatarioId();
        this.dataCompra = conferido.dataCompra();
        this.mesReferencia = conferido.mesReferencia();
        this.cartaoId = conferido.cartaoId();
        this.faturaId = conferido.faturaId();
    }

    LancamentoRegistrado paraConsulta() {
        return new LancamentoRegistrado(id, tipo, valor, descricao, categoriaId, destinatarioId,
                dataCompra, mesReferencia, cartaoId, faturaId, origem);
    }
}
