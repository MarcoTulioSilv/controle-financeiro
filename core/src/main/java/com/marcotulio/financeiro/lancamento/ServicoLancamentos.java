package com.marcotulio.financeiro.lancamento;

import com.marcotulio.financeiro.cadastro.CategoriaCadastrada;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cartao.FaturaRegistrada;
import com.marcotulio.financeiro.cartao.ServicoFaturas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de entrada pública dos lançamentos (RF01, RF13, RN02).
 *
 * <p>O serviço busca o que as regras precisam nos outros módulos, sempre pelos serviços públicos
 * deles (M5), e deixa a decisão com as {@link RegrasDoLancamento}. O autor da gravação não é
 * parâmetro: a auditoria (C7) registra o usuário logado.
 *
 * <p>Não há exclusão: no MVP 1 um lançamento só é registrado e editado.
 */
@Service
public class ServicoLancamentos {

    private final RepositorioDeLancamentos lancamentos;
    private final ServicoCategorias categorias;
    private final ServicoDestinatarios destinatarios;
    private final ServicoFaturas faturas;

    ServicoLancamentos(
            RepositorioDeLancamentos lancamentos,
            ServicoCategorias categorias,
            ServicoDestinatarios destinatarios,
            ServicoFaturas faturas) {
        this.lancamentos = lancamentos;
        this.categorias = categorias;
        this.destinatarios = destinatarios;
        this.faturas = faturas;
    }

    /**
     * Confere e grava um lançamento novo.
     *
     * @param origem {@code WEB} (formulário) ou {@code IMPORTACAO} (planilha)
     * @throws IllegalArgumentException com a mensagem para o usuário, se algum dado for inválido
     */
    @Transactional
    public LancamentoRegistrado registrar(DadosDoLancamento dados, OrigemLancamento origem) {
        if (origem == null) {
            throw new IllegalArgumentException("Informe a origem do lançamento.");
        }
        Lancamento novo = new Lancamento(conferir(dados), origem);
        return lancamentos.save(novo).paraConsulta();
    }

    /**
     * Troca os dados de um lançamento, conferidos de novo como num registro: o mês de referência
     * e a fatura são recalculados. A origem continua a do registro.
     *
     * @throws IllegalArgumentException se o lançamento não existir ou algum dado for inválido
     */
    @Transactional
    public LancamentoRegistrado editar(long lancamentoId, DadosDoLancamento dados) {
        Lancamento lancamento = lancamentos.findById(lancamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lançamento não encontrado: " + lancamentoId + "."));
        lancamento.alterar(conferir(dados));
        return lancamento.paraConsulta();
    }

    /**
     * Com cartão, a fatura é obtida (e criada, se ainda não existir) antes da decisão sobre o mês
     * de referência, porque o mês calculado é o dela. Se o mês informado for outro, a fatura
     * criada fica vazia e aberta, à espera das próximas compras daquele ciclo.
     */
    private LancamentoConferido conferir(DadosDoLancamento dados) {
        RegrasDoLancamento.exigirCamposObrigatorios(dados);
        CategoriaCadastrada categoria = categorias.buscarPorId(dados.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria não encontrada: " + dados.categoriaId() + "."));
        if (destinatarios.buscarPorId(dados.destinatarioId()).isEmpty()) {
            throw new IllegalArgumentException("Para quem não encontrado: " + dados.destinatarioId() + ".");
        }
        FaturaRegistrada fatura = dados.cartaoId() == null
                ? null
                : faturas.faturaDaCompra(dados.cartaoId(), dados.dataCompra());
        return RegrasDoLancamento.conferir(dados, categoria, fatura);
    }
}
