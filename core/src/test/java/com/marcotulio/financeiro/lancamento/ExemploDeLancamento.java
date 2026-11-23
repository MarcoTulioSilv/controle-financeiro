package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Monta os dados de um lançamento válido para os testes: cada teste muda só o campo que quer
 * provar. Os valores são fictícios (ERS): uma despesa de mercado de R$ 150,00 em 10/10/2026.
 */
final class ExemploDeLancamento {

    private TipoLancamento tipo = TipoLancamento.DESPESA;
    private BigDecimal valor = new BigDecimal("150.00");
    private String descricao = "Mercado";
    private Long categoriaId;
    private Long destinatarioId;
    private LocalDate dataCompra = LocalDate.of(2026, 10, 10);
    private Long cartaoId;
    private YearMonth mesReferenciaInformado;

    ExemploDeLancamento(Long categoriaId, Long destinatarioId) {
        this.categoriaId = categoriaId;
        this.destinatarioId = destinatarioId;
    }

    ExemploDeLancamento tipo(TipoLancamento novo) {
        tipo = novo;
        return this;
    }

    ExemploDeLancamento valor(String novo) {
        valor = novo == null ? null : new BigDecimal(novo);
        return this;
    }

    ExemploDeLancamento descricao(String nova) {
        descricao = nova;
        return this;
    }

    ExemploDeLancamento categoria(Long nova) {
        categoriaId = nova;
        return this;
    }

    ExemploDeLancamento paraQuem(Long novo) {
        destinatarioId = novo;
        return this;
    }

    ExemploDeLancamento dataCompra(LocalDate nova) {
        dataCompra = nova;
        return this;
    }

    ExemploDeLancamento cartao(Long novo) {
        cartaoId = novo;
        return this;
    }

    ExemploDeLancamento mesReferencia(YearMonth informado) {
        mesReferenciaInformado = informado;
        return this;
    }

    DadosDoLancamento dados() {
        return new DadosDoLancamento(tipo, valor, descricao, categoriaId, destinatarioId, dataCompra,
                cartaoId, mesReferenciaInformado);
    }
}
