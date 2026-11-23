package com.marcotulio.financeiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import com.marcotulio.financeiro.cartao.CompraNoFechamento;
import com.marcotulio.financeiro.cartao.RegraCartao;
import com.marcotulio.financeiro.cartao.ServicoCartoes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Registro de dívidas e geração das parcelas gravando de verdade no MySQL (RF12, ADR-005).
 *
 * <p>Como o {@code ServicoLancamentosTest}, esta classe não é {@code @Transactional}: as faturas
 * das parcelas são criadas em transações próprias, que precisam enxergar o cartão já confirmado
 * no banco. O {@code @AfterEach} apaga o que foi gravado (só no teste: a aplicação nunca exclui).
 */
class ServicoDividasTest extends TesteDeIntegracao {

    @Autowired
    private ServicoDividas servico;

    @Autowired
    private ServicoLancamentos lancamentos;

    @Autowired
    private ServicoCategorias categorias;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private ServicoCartoes cartoes;

    @Autowired
    private JdbcClient jdbc;

    private long dividas;
    private long marco;

    /** Banco B do Marco: fecha no dia 5 e vence no dia 12 (ERS, Apêndice B). */
    private long bmt;

    @BeforeEach
    void cadastraOQueUmaDividaUsa() {
        dividas = categorias.cadastrarSeAusente("Dívidas", TipoCategoria.DESPESA, true).id();
        marco = destinatarios.cadastrarSeAusente("Marco", "@mt").id();
        usuarios.cadastrarSeAusente("Marco", "marco", "senha-de-teste", "@mt");
        bmt = cartoes.cadastrarSeAusente(
                "bmt", "Banco B", "marco", new RegraCartao(5, 12, CompraNoFechamento.PROXIMA)).id();
    }

    @AfterEach
    void apagaOQueOTesteGravou() {
        jdbc.sql("DELETE FROM lancamento").update();
        jdbc.sql("DELETE FROM divida").update();
        jdbc.sql("DELETE FROM fatura").update();
        jdbc.sql("DELETE FROM cartao").update();
        jdbc.sql("DELETE FROM usuario WHERE login = 'marco'").update();
        jdbc.sql("DELETE FROM destinatario WHERE marcacao_bot = '@mt'").update();
        jdbc.sql("DELETE FROM categoria WHERE nome = 'Dívidas'").update();
    }

    /** {@code celular 16x 106,18 bmt @mt}, comprado em 10/10/2026 (ERS, seção 4.1). */
    private ExemploDeDivida celular() {
        return new ExemploDeDivida(marco, bmt);
    }

    // ----- registro e parcelas -----

    @Test
    void parcelamentoNoCartaoGeraUmaParcelaPorMesAPartirDaFaturaCalculada() {
        // Act: 10/10/2026 é depois do fechamento do dia 5; a fatura é a que vence em 12/11/2026.
        DividaRegistrada divida = servico.registrar(celular().dados(), OrigemLancamento.WEB);

        // Assert: a dívida
        assertThat(divida.id()).isPositive();
        assertThat(divida.tipo()).isEqualTo(TipoDivida.PARCELAMENTO_CARTAO);
        assertThat(divida.descricao()).isEqualTo("celular");
        assertThat(divida.valorTotal()).isEqualTo("1698.88");
        assertThat(divida.quantidadeParcelas()).isEqualTo(16);
        assertThat(divida.status()).isEqualTo(StatusDivida.ATIVA);
        Map<String, Object> linha = jdbc.sql("SELECT * FROM divida WHERE id = ?")
                .param(divida.id()).query().singleRow();
        assertThat(linha.get("valor_total")).hasToString("1698.88");
        assertThat(linha.get("cartao_id")).isEqualTo(bmt);
        assertThat(linha.get("destinatario_id")).isEqualTo(marco);
        assertThat(linha.get("data_contratacao")).hasToString("2026-10-10");
        assertThat(linha.get("status")).isEqualTo("ATIVA");
        assertThat(linha.get("data_quitacao")).isNull();

        // Assert: as parcelas, de nov/2026 a fev/2028
        List<LancamentoRegistrado> parcelas = divida.parcelas();
        assertThat(parcelas).hasSize(16);
        for (int i = 0; i < parcelas.size(); i++) {
            LancamentoRegistrado parcela = parcelas.get(i);
            int numero = i + 1;
            assertThat(parcela.numeroParcela()).isEqualTo(numero);
            assertThat(parcela.dividaId()).isEqualTo(divida.id());
            assertThat(parcela.descricao()).isEqualTo("celular " + numero + "/16");
            assertThat(parcela.valor()).isEqualTo("106.18");
            assertThat(parcela.tipo()).isEqualTo(TipoLancamento.DESPESA);
            assertThat(parcela.categoriaId()).isEqualTo(dividas);
            assertThat(parcela.destinatarioId()).isEqualTo(marco);
            assertThat(parcela.dataCompra()).isEqualTo(LocalDate.of(2026, 10, 10));
            assertThat(parcela.cartaoId()).isEqualTo(bmt);
            assertThat(parcela.origem()).isEqualTo(OrigemLancamento.WEB);
            assertThat(parcela.mesReferencia()).isEqualTo(YearMonth.of(2026, 11).plusMonths(i));
        }
        assertThat(parcelas.getLast().mesReferencia()).isEqualTo(YearMonth.of(2028, 2));
    }

    @Test
    void cadaParcelaNoCartaoFicaNaFaturaDoProprioMes() {
        // Act
        DividaRegistrada divida = servico.registrar(celular().quantidade(3).dados(), OrigemLancamento.WEB);

        // Assert: o mês do lançamento e o da fatura nunca divergem (modelo de dados, seção 3.7).
        var linhas = jdbc.sql("""
                        SELECT l.numero_parcela, l.mes_referencia, f.mes_referencia, f.data_fechamento,
                               f.data_vencimento
                          FROM lancamento l JOIN fatura f ON f.id = l.fatura_id
                         WHERE l.divida_id = ?
                         ORDER BY l.numero_parcela
                        """)
                .param(divida.id())
                .query((rs, n) -> rs.getInt(1) + " " + rs.getString(2) + " " + rs.getString(3)
                        + " " + rs.getString(4) + " " + rs.getString(5))
                .list();
        assertThat(linhas).containsExactly(
                "1 2026-11-01 2026-11-01 2026-11-05 2026-11-12",
                "2 2026-12-01 2026-12-01 2026-12-05 2026-12-12",
                "3 2027-01-01 2027-01-01 2027-01-05 2027-01-12");
    }

    @Test
    void informadoPeloTotalADiferencaFicaNaPrimeiraEASomaGravadaEhExatamenteOTotal() {
        // Act: R$ 1.000,00 em 3x.
        DividaRegistrada divida = servico.registrar(
                celular().totalDe("1000.00").quantidade(3).dados(), OrigemLancamento.WEB);

        // Assert
        assertThat(divida.parcelas()).extracting(LancamentoRegistrado::valor).containsExactly(
                new BigDecimal("333.34"), new BigDecimal("333.33"), new BigDecimal("333.33"));
        BigDecimal somaNoBanco = jdbc.sql("SELECT SUM(valor) FROM lancamento WHERE divida_id = ?")
                .param(divida.id())
                .query(BigDecimal.class)
                .single();
        assertThat(somaNoBanco).isEqualTo("1000.00");
        assertThat(divida.valorTotal()).isEqualTo("1000.00");
    }

    @Test
    void financiamentoSemCartaoComecaNoMesDaContratacao() {
        // Arrange
        DadosDaDivida financiamento = celular()
                .tipo(TipoDivida.FINANCIAMENTO)
                .descricao("moto")
                .cartao(null)
                .totalDe("12000.00")
                .quantidade(24)
                .dataContratacao(LocalDate.of(2026, 12, 15))
                .dados();

        // Act
        DividaRegistrada divida = servico.registrar(financiamento, OrigemLancamento.WEB);

        // Assert: sem cartão vale a regra do RF01, o mês da compra; as demais nos meses seguintes.
        assertThat(divida.cartaoId()).isNull();
        assertThat(divida.parcelas()).hasSize(24).allSatisfy(parcela -> {
            assertThat(parcela.cartaoId()).isNull();
            assertThat(parcela.faturaId()).isNull();
            assertThat(parcela.valor()).isEqualTo("500.00");
        });
        assertThat(divida.parcelas().getFirst().mesReferencia()).isEqualTo(YearMonth.of(2026, 12));
        assertThat(divida.parcelas().getLast().mesReferencia()).isEqualTo(YearMonth.of(2028, 11));
        assertThat(divida.parcelas().getLast().descricao()).isEqualTo("moto 24/24");
    }

    @Test
    void parcelasImportadasDaPlanilhaGuardamAOrigem() {
        // Act
        DividaRegistrada divida = servico.registrar(celular().quantidade(2).dados(), OrigemLancamento.IMPORTACAO);

        // Assert
        assertThat(divida.parcelas()).allSatisfy(
                parcela -> assertThat(parcela.origem()).isEqualTo(OrigemLancamento.IMPORTACAO));
    }

    // ----- recusas -----

    @Test
    void recusaDividaSemOrigem() {
        assertThatThrownBy(() -> servico.registrar(celular().dados(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a origem da dívida.");
        assertThat(contarDividas()).isZero();
    }

    @Test
    void recusaDadosIncompletos() {
        assertThatThrownBy(() -> servico.registrar(celular().paraQuem(null).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe para quem é a dívida.");
        assertThat(contarDividas()).isZero();
    }

    @Test
    void recusaParaQuemInexistente() {
        assertThatThrownBy(() -> servico.registrar(celular().paraQuem(-1L).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Para quem não encontrado: -1.");
        assertThat(contarDividas()).isZero();
    }

    @Test
    void semACategoriaDividasNadaEhGravado() {
        // Arrange
        jdbc.sql("DELETE FROM categoria WHERE nome = 'Dívidas'").update();

        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(celular().dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dívidas");
        assertThat(contarDividas()).isZero();
    }

    @Test
    void recusaCartaoInexistente() {
        assertThatThrownBy(() -> servico.registrar(celular().cartao(-1L).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cartão não encontrado");
        assertThat(contarDividas()).isZero();
    }

    @Test
    void parcelaRecusadaDesfazTudoInclusiveADivida() {
        // Arrange: 196 caracteres cabem na dívida (limite de 200), mas com o sufixo " 1/16" a
        // parcela 1 fica com 201 e é recusada depois de a dívida já ter sido gravada.
        DadosDaDivida descricaoLonga = celular().descricao("x".repeat(196)).dados();

        // Act e Assert: a dívida e as parcelas são gravadas numa única transação.
        assertThatThrownBy(() -> servico.registrar(descricaoLonga, OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no máximo 200 caracteres");
        assertThat(contarDividas()).isZero();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM lancamento").query(Integer.class).single()).isZero();
    }

    // ----- saldo devedor -----

    @Test
    void saldoDevedorAntesDaPrimeiraParcelaEhOTotal() {
        // Arrange
        DividaRegistrada divida = servico.registrar(celular().dados(), OrigemLancamento.WEB);

        // Act: em outubro de 2026, a primeira parcela (novembro) ainda não chegou.
        BigDecimal saldo = servico.saldoDevedor(divida.id(), YearMonth.of(2026, 10));

        // Assert
        assertThat(saldo).isEqualTo("1698.88");
    }

    @Test
    void saldoDevedorCaiUmaParcelaPorMes() {
        // Arrange
        DividaRegistrada divida = servico.registrar(celular().dados(), OrigemLancamento.WEB);

        // Act: a parcela do próprio mês conta como paga (maquinas-estado.md, seção 2).
        BigDecimal emNovembro = servico.saldoDevedor(divida.id(), YearMonth.of(2026, 11));
        BigDecimal emDezembro = servico.saldoDevedor(divida.id(), YearMonth.of(2026, 12));

        // Assert: 15 e 14 parcelas de R$ 106,18.
        assertThat(emNovembro).isEqualTo("1592.70");
        assertThat(emDezembro).isEqualTo("1486.52");
    }

    @Test
    void saldoDevedorNoMesDaUltimaParcelaEDepoisEhZero() {
        // Arrange
        DividaRegistrada divida = servico.registrar(celular().dados(), OrigemLancamento.WEB);

        // Act
        BigDecimal naUltima = servico.saldoDevedor(divida.id(), YearMonth.of(2028, 2));
        BigDecimal depois = servico.saldoDevedor(divida.id(), YearMonth.of(2030, 1));

        // Assert
        assertThat(naUltima).isEqualByComparingTo("0.00");
        assertThat(depois).isEqualByComparingTo("0.00");
    }

    @Test
    void saldoDevedorAcompanhaOValorDasParcelasNoBanco() {
        // Arrange: o saldo vem das parcelas, não de uma coluna (modelo de dados, seção 3.5).
        // Aqui uma parcela muda direto no banco, como fará o abatimento da T16.
        DividaRegistrada divida = servico.registrar(
                celular().totalDe("1000.00").quantidade(3).dados(), OrigemLancamento.WEB);
        jdbc.sql("UPDATE lancamento SET valor = 100.00 WHERE divida_id = ? AND numero_parcela = 3")
                .param(divida.id())
                .update();

        // Act
        BigDecimal saldo = servico.saldoDevedor(divida.id(), YearMonth.of(2026, 11));

        // Assert: parcelas 2 e 3, de R$ 333,33 e R$ 100,00.
        assertThat(saldo).isEqualTo("433.33");
    }

    @Test
    void saldoDevedorDeDividaInexistenteFalha() {
        assertThatThrownBy(() -> servico.saldoDevedor(-1L, YearMonth.of(2026, 11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dívida não encontrada: -1.");
    }

    // ----- parcelas não são editadas como lançamentos avulsos -----

    @Test
    void parcelaNaoEhEditadaComoUmLancamentoAvulso() {
        // Arrange: editar a parcela 2 recalcularia o mês pela data da compra e a tiraria do lugar.
        DividaRegistrada divida = servico.registrar(celular().quantidade(2).dados(), OrigemLancamento.WEB);
        LancamentoRegistrado segunda = divida.parcelas().get(1);
        DadosDoLancamento edicao = new ExemploDeLancamento(dividas, marco)
                .descricao("celular 2/2").dataCompra(LocalDate.of(2026, 10, 10)).dados();

        // Act e Assert
        assertThatThrownBy(() -> lancamentos.editar(segunda.id(), edicao))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parcela 2 da dívida");
        String mes = jdbc.sql("SELECT mes_referencia FROM lancamento WHERE id = ?")
                .param(segunda.id())
                .query(String.class)
                .single();
        assertThat(mes).isEqualTo("2026-12-01");
    }

    private int contarDividas() {
        return jdbc.sql("SELECT COUNT(*) FROM divida").query(Integer.class).single();
    }
}
