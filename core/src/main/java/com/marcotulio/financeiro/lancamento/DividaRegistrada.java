package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma dívida gravada, como os outros módulos a enxergam: só os dados, sem a entidade JPA (M5).
 *
 * @param valorTotal a soma exata das parcelas (ADR-005)
 * @param cartaoId {@code null} quando a dívida não é paga no cartão
 * @param destinatarioId o "para quem" (RF13) de todas as parcelas
 * @param parcelas da primeira à última, na ordem do número da parcela
 */
public record DividaRegistrada(
        long id,
        TipoDivida tipo,
        String descricao,
        BigDecimal valorTotal,
        int quantidadeParcelas,
        Long cartaoId,
        long destinatarioId,
        LocalDate dataContratacao,
        StatusDivida status,
        List<LancamentoRegistrado> parcelas) {

    public DividaRegistrada {
        parcelas = List.copyOf(parcelas);
    }
}
