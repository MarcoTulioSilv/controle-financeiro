package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que quem registra uma dívida informa, do jeito que chegou: o {@link ServicoDividas} confere
 * tudo antes de gravar.
 *
 * <p>O valor vem de um jeito ou de outro, nunca dos dois (ADR-005): pelo valor da parcela, como
 * no bot ({@code 16x 106,18}), ou pelo valor total, como no formulário web.
 *
 * @param descricao o item, sem o número da parcela (ex.: "celular")
 * @param valorParcela informado quando o usuário sabe o valor de cada parcela; senão, {@code null}
 * @param valorTotal informado quando o usuário sabe o total; senão, {@code null}
 * @param destinatarioId o "para quem" (RF13) de todas as parcelas: obrigatório, sem valor padrão
 * @param dataContratacao a data da compra parcelada ou da contratação do financiamento
 * @param cartaoId obrigatório no parcelamento de cartão; opcional nos demais tipos
 */
public record DadosDaDivida(
        TipoDivida tipo,
        String descricao,
        BigDecimal valorParcela,
        BigDecimal valorTotal,
        Integer quantidadeParcelas,
        Long destinatarioId,
        LocalDate dataContratacao,
        Long cartaoId) {
}
