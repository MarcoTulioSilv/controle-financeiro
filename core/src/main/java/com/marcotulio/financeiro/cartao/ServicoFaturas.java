package com.marcotulio.financeiro.cartao;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública das faturas de cartão (RF14, RF16, RN03). */
@Service
public class ServicoFaturas {

    private final RepositorioDeCartoes cartoes;
    private final RepositorioDeFaturas faturas;
    private final CriacaoDeFatura criacao;
    private final ServicoFeriados feriados;
    private final CalculadoraFatura calculadora = new CalculadoraFatura();
    private final CalendarioBancario calendario = new CalendarioBancario();

    ServicoFaturas(
            RepositorioDeCartoes cartoes,
            RepositorioDeFaturas faturas,
            CriacaoDeFatura criacao,
            ServicoFeriados feriados) {
        this.cartoes = cartoes;
        this.faturas = faturas;
        this.criacao = criacao;
        this.feriados = feriados;
    }

    /**
     * A fatura em que entra uma compra feita no cartão na {@code dataCompra}, criada (aberta) se
     * ainda não existir. O mês de referência é o do vencimento nominal; o vencimento efetivo é o
     * primeiro dia útil a partir dele, considerando os feriados gravados (RN03).
     *
     * @throws IllegalArgumentException se o cartão não existir
     */
    @Transactional
    public FaturaRegistrada faturaDaCompra(long cartaoId, LocalDate dataCompra) {
        Cartao cartao = cartoes.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado: " + cartaoId));
        FaturaCalculada calculada = calculadora.calcular(cartao.regra(), dataCompra);

        Optional<Fatura> existente = faturas.findByCartaoIdAndMesReferencia(cartaoId, calculada.mesReferencia());
        if (existente.isPresent()) {
            return existente.get().paraConsulta();
        }
        try {
            return criacao.criar(cartaoId, calculada, vencimentoEfetivo(calculada.vencimento()));
        } catch (DataIntegrityViolationException corrida) {
            // Outra compra criou a mesma fatura entre a busca acima e o INSERT, e o UNIQUE
            // (cartao_id, mes_referencia) recusou a segunda. A fatura dela é a nossa.
            return faturas.buscarUltimaVersaoConfirmada(cartaoId, calculada.mesReferencia())
                    .map(Fatura::paraConsulta)
                    .orElseThrow(() -> corrida);
        }
    }

    /**
     * Um mês adiante basta como janela de feriados: nem o Carnaval junta tantos dias seguidos
     * sem expediente bancário.
     */
    private LocalDate vencimentoEfetivo(LocalDate vencimentoNominal) {
        var feriadosProximos = feriados.feriadosEntre(vencimentoNominal, vencimentoNominal.plusMonths(1));
        return calendario.proximoDiaUtil(vencimentoNominal, feriadosProximos);
    }
}
