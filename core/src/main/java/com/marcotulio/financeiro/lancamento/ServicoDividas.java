package com.marcotulio.financeiro.lancamento;

import com.marcotulio.financeiro.cadastro.CategoriaCadastrada;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import com.marcotulio.financeiro.cartao.FaturaRegistrada;
import com.marcotulio.financeiro.cartao.ServicoFaturas;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de entrada pública das dívidas: compras parceladas, financiamentos e empréstimos (RF12).
 *
 * <p>Registrar uma dívida grava a dívida e todas as parcelas numa única transação: ou tudo é
 * gravado, ou nada. Cada parcela é um lançamento comum, conferido pelas
 * {@link RegrasDoLancamento} como qualquer outro. No MVP 1 a dívida só existe {@code ATIVA}.
 */
@Service
public class ServicoDividas {

    /** Toda parcela, de compra parcelada ou de financiamento, vai para esta categoria (RF12). */
    static final String CATEGORIA_DAS_PARCELAS = "Dívidas";

    private final RepositorioDeDividas dividas;
    private final RepositorioDeLancamentos lancamentos;
    private final ServicoCategorias categorias;
    private final ServicoDestinatarios destinatarios;
    private final ServicoFaturas faturas;

    ServicoDividas(
            RepositorioDeDividas dividas,
            RepositorioDeLancamentos lancamentos,
            ServicoCategorias categorias,
            ServicoDestinatarios destinatarios,
            ServicoFaturas faturas) {
        this.dividas = dividas;
        this.lancamentos = lancamentos;
        this.categorias = categorias;
        this.destinatarios = destinatarios;
        this.faturas = faturas;
    }

    /**
     * Confere e grava a dívida e as parcelas. A primeira parcela entra no mês da fatura calculada
     * para a data da compra (com cartão) ou no mês da compra (sem cartão, RF01); cada uma das
     * demais, no mês seguinte ao da anterior, na fatura daquele mês.
     *
     * @param origem {@code WEB} (formulário) ou {@code IMPORTACAO} (planilha), herdada pelas parcelas
     * @throws IllegalArgumentException com a mensagem para o usuário, se algum dado for inválido
     * @throws IllegalStateException se a categoria Dívidas não estiver cadastrada
     */
    @Transactional
    public DividaRegistrada registrar(DadosDaDivida dados, OrigemLancamento origem) {
        if (origem == null) {
            throw new IllegalArgumentException("Informe a origem da dívida.");
        }
        RegrasDaDivida.exigirCamposObrigatorios(dados);
        List<BigDecimal> valores = RegrasDaDivida.valoresDasParcelas(dados);
        CategoriaCadastrada categoria = categoriaDasParcelas();
        if (destinatarios.buscarPorId(dados.destinatarioId()).isEmpty()) {
            throw new IllegalArgumentException("Para quem não encontrado: " + dados.destinatarioId() + ".");
        }

        // Buscada antes de gravar a dívida: um cartão inexistente é recusado com mensagem clara.
        FaturaRegistrada faturaDaPrimeira = dados.cartaoId() == null
                ? null
                : faturas.faturaDaCompra(dados.cartaoId(), dados.dataContratacao());
        YearMonth mesDaPrimeira = faturaDaPrimeira == null
                ? YearMonth.from(dados.dataContratacao())
                : faturaDaPrimeira.mesReferencia();

        String descricao = dados.descricao().strip();
        BigDecimal total = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Divida divida = dividas.save(new Divida(dados, descricao, total));

        List<LancamentoRegistrado> parcelas = new ArrayList<>();
        for (int numero = 1; numero <= valores.size(); numero++) {
            YearMonth mes = mesDaPrimeira.plusMonths(numero - 1L);
            FaturaRegistrada fatura = faturaDaParcela(dados.cartaoId(), numero, faturaDaPrimeira, mes);
            // O mês da parcela vai como mês informado: assim as RegrasDoLancamento o usam no
            // lugar do mês da compra, e ligam a parcela à fatura quando os dois meses coincidem.
            DadosDoLancamento parcela = new DadosDoLancamento(
                    TipoLancamento.DESPESA,
                    valores.get(numero - 1),
                    RegrasDaDivida.descricaoDaParcela(descricao, numero, valores.size()),
                    categoria.id(),
                    dados.destinatarioId(),
                    dados.dataContratacao(),
                    dados.cartaoId(),
                    mes);
            LancamentoConferido conferida = RegrasDoLancamento.conferir(parcela, categoria, fatura);
            Lancamento gravada = lancamentos.save(new Lancamento(conferida, origem, divida.id(), numero));
            parcelas.add(gravada.paraConsulta());
        }
        return divida.paraConsulta(parcelas);
    }

    /**
     * O saldo devedor da dívida no {@code mes}: a soma das parcelas com mês de referência
     * posterior a ele. A parcela do próprio mês conta como paga ({@code maquinas-estado.md},
     * seção 2). É sempre calculado, nunca gravado (modelo de dados, seção 3.5).
     *
     * @throws IllegalArgumentException se a dívida não existir
     */
    @Transactional(readOnly = true)
    public BigDecimal saldoDevedor(long dividaId, YearMonth mes) {
        if (!dividas.existsById(dividaId)) {
            throw new IllegalArgumentException("Dívida não encontrada: " + dividaId + ".");
        }
        return lancamentos.somarParcelasPosterioresA(dividaId, mes.atDay(1));
    }

    private CategoriaCadastrada categoriaDasParcelas() {
        return categorias.buscarPorNomeETipo(CATEGORIA_DAS_PARCELAS, TipoCategoria.DESPESA)
                .orElseThrow(() -> new IllegalStateException(
                        "A categoria de despesa " + CATEGORIA_DAS_PARCELAS + " não está cadastrada:"
                                + " confira o arquivo de dados iniciais."));
    }

    /** Sem cartão, nenhuma; a primeira, a da data da compra; as demais, a do mês de cada uma. */
    private FaturaRegistrada faturaDaParcela(
            Long cartaoId, int numero, FaturaRegistrada faturaDaPrimeira, YearMonth mes) {
        if (cartaoId == null) {
            return null;
        }
        return numero == 1 ? faturaDaPrimeira : faturas.faturaDoMes(cartaoId, mes);
    }
}
