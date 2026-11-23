package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Monta os dados de uma dívida válida para os testes: cada teste muda só o campo que quer provar.
 * O padrão é o exemplo do ERS (seção 4.1): {@code celular 16x 106,18 bmt}, comprado em
 * 10/10/2026.
 */
final class ExemploDeDivida {

    private TipoDivida tipo = TipoDivida.PARCELAMENTO_CARTAO;
    private String descricao = "celular";
    private BigDecimal valorParcela = new BigDecimal("106.18");
    private BigDecimal valorTotal;
    private Integer quantidadeParcelas = 16;
    private Long destinatarioId;
    private LocalDate dataContratacao = LocalDate.of(2026, 10, 10);
    private Long cartaoId;

    ExemploDeDivida(Long destinatarioId, Long cartaoId) {
        this.destinatarioId = destinatarioId;
        this.cartaoId = cartaoId;
    }

    ExemploDeDivida tipo(TipoDivida novo) {
        tipo = novo;
        return this;
    }

    ExemploDeDivida descricao(String nova) {
        descricao = nova;
        return this;
    }

    /** Informa o valor de cada parcela (como no bot: {@code 16x 106,18}) e apaga o total. */
    ExemploDeDivida parcelasDe(String valor) {
        valorParcela = valor == null ? null : new BigDecimal(valor);
        valorTotal = null;
        return this;
    }

    /** Informa o valor total (como na web) e apaga o valor da parcela. */
    ExemploDeDivida totalDe(String valor) {
        valorTotal = valor == null ? null : new BigDecimal(valor);
        valorParcela = null;
        return this;
    }

    /** Informa os dois valores ao mesmo tempo, o que as regras recusam. */
    ExemploDeDivida parcelaETotal(String parcela, String total) {
        valorParcela = new BigDecimal(parcela);
        valorTotal = new BigDecimal(total);
        return this;
    }

    ExemploDeDivida quantidade(Integer nova) {
        quantidadeParcelas = nova;
        return this;
    }

    ExemploDeDivida paraQuem(Long novo) {
        destinatarioId = novo;
        return this;
    }

    ExemploDeDivida dataContratacao(LocalDate nova) {
        dataContratacao = nova;
        return this;
    }

    ExemploDeDivida cartao(Long novo) {
        cartaoId = novo;
        return this;
    }

    DadosDaDivida dados() {
        return new DadosDaDivida(tipo, descricao, valorParcela, valorTotal, quantidadeParcelas,
                destinatarioId, dataContratacao, cartaoId);
    }
}
