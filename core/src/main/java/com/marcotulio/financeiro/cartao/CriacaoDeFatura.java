package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava uma fatura nova numa transação só dela ({@code REQUIRES_NEW}).
 *
 * <p>Por que uma transação separada: se o UNIQUE {@code (cartao_id, mes_referencia)} recusar o
 * INSERT, o erro desfaz só esta transação curta. A transação de quem chamou (por exemplo, a do
 * lançamento) continua saudável e pode buscar a fatura que a outra compra criou. Se o INSERT
 * falhasse dentro da transação de quem chamou, o Spring a marcaria para desfazer tudo no final.
 *
 * <p>Fica numa classe à parte porque o {@code @Transactional} só vale em chamadas que passam pelo
 * proxy do Spring: um método chamado de dentro da mesma classe ignoraria o {@code REQUIRES_NEW}.
 */
@Component
class CriacaoDeFatura {

    private final RepositorioDeFaturas faturas;

    CriacaoDeFatura(RepositorioDeFaturas faturas) {
        this.faturas = faturas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FaturaRegistrada criar(long cartaoId, FaturaCalculada calculada, LocalDate vencimentoEfetivo) {
        return faturas.saveAndFlush(new Fatura(cartaoId, calculada, vencimentoEfetivo)).paraConsulta();
    }
}
