package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class RegraCartaoTest {

    @Test
    void aceitaOsDiasLimiteDoMes() {
        // Arrange (preparar) + Act (agir)
        RegraCartao regra = new RegraCartao(1, 31, CompraNoFechamento.ATUAL);

        // Assert (verificar)
        assertThat(regra.diaFechamento()).isEqualTo(1);
        assertThat(regra.diaVencimento()).isEqualTo(31);
    }

    @Test
    void rejeitaDiaDeFechamentoAbaixoDoPrimeiro() {
        // Arrange (preparar) + Act (agir) + Assert (verificar)
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegraCartao(0, 5, CompraNoFechamento.ATUAL))
                .withMessageContaining("dia de fechamento");
    }

    @Test
    void rejeitaDiaDeFechamentoAcimaDoTrigesimoPrimeiro() {
        // Arrange (preparar) + Act (agir) + Assert (verificar)
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegraCartao(32, 5, CompraNoFechamento.ATUAL))
                .withMessageContaining("dia de fechamento");
    }

    @Test
    void rejeitaDiaDeVencimentoForaDoMes() {
        // Arrange (preparar) + Act (agir) + Assert (verificar)
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegraCartao(25, 32, CompraNoFechamento.ATUAL))
                .withMessageContaining("dia de vencimento");
    }

    @Test
    void rejeitaRegraDeCompraNoFechamentoNula() {
        // Arrange (preparar) + Act (agir) + Assert (verificar)
        assertThatNullPointerException()
                .isThrownBy(() -> new RegraCartao(25, 5, null))
                .withMessageContaining("compra no dia do fechamento");
    }
}
