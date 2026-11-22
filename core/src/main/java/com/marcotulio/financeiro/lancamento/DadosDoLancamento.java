package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * O que quem lança informa, do jeito que chegou: o {@link ServicoLancamentos} confere tudo antes
 * de gravar. Os mesmos dados servem para registrar e para editar.
 *
 * @param destinatarioId o "para quem" (RF13): obrigatório, sem valor padrão
 * @param cartaoId opcional; {@code null} quando a compra não foi no cartão
 * @param mesReferenciaInformado opcional; quando informado, prevalece sobre o cálculo (RF01)
 */
public record DadosDoLancamento(
        TipoLancamento tipo,
        BigDecimal valor,
        String descricao,
        Long categoriaId,
        Long destinatarioId,
        LocalDate dataCompra,
        Long cartaoId,
        YearMonth mesReferenciaInformado) {
}
