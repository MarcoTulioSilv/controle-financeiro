package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A fatura em que uma compra no cartão entra, com o vencimento nominal e o efetivo (RF14, RF16,
 * RN03). As datas usam os cartões fictícios do ERS, Apêndice B.
 *
 * <p>Diferente dos outros testes de integração, esta classe não é {@code @Transactional}: o
 * serviço cria a fatura numa transação própria, que precisa enxergar o cartão já confirmado no
 * banco, e o teste da corrida usa duas transações de verdade. Por isso cada teste grava de fato,
 * e o {@code @AfterEach} apaga o que foi gravado (só no teste: a aplicação nunca exclui).
 */
class ServicoFaturasTest extends TesteDeIntegracao {

    @Autowired
    private ServicoFaturas servico;

    @Autowired
    private ServicoCartoes cartoes;

    @Autowired
    private ServicoFeriados feriados;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager gerenciadorDeTransacoes;

    /** Banco A do Marco: fecha no dia 20 e vence no dia 25. */
    private long amt;

    /** Banco C do Marco: fecha no dia 15 e vence no dia 25. */
    private long cmt;

    @BeforeEach
    void cadastraOsCartoes() {
        destinatarios.cadastrarSeAusente("Marco", "@mt");
        usuarios.cadastrarSeAusente("Marco", "marco", "senha-de-teste", "@mt");
        amt = cartoes.cadastrarSeAusente(
                "amt", "Banco A", "marco", new RegraCartao(20, 25, CompraNoFechamento.PROXIMA)).id();
        cmt = cartoes.cadastrarSeAusente(
                "cmt", "Banco C", "marco", new RegraCartao(15, 25, CompraNoFechamento.PROXIMA)).id();
        feriados.gravarFeriadosNacionais(2026);
    }

    @AfterEach
    void apagaOQueOTesteGravou() {
        jdbc.sql("DELETE FROM fatura").update();
        jdbc.sql("DELETE FROM cartao").update();
        jdbc.sql("DELETE FROM usuario WHERE login = 'marco'").update();
        jdbc.sql("DELETE FROM destinatario WHERE marcacao_bot = '@mt'").update();
        jdbc.sql("DELETE FROM feriado WHERE abrangencia = 'LOCAL'").update();
    }

    @Test
    void criaAFaturaDaCompraComMesDeReferenciaDoVencimentoNominal() {
        // Act: compra em 10/10/2026 no cartão que fecha no dia 20 e vence no dia 25.
        FaturaRegistrada fatura = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 10));

        // Assert
        assertThat(fatura.id()).isPositive();
        assertThat(fatura.cartaoId()).isEqualTo(amt);
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(fatura.dataFechamento()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(fatura.dataVencimento()).isEqualTo(LocalDate.of(2026, 10, 25));
    }

    @Test
    void vencimentoNoDomingoTemVencimentoEfetivoNaSegunda() {
        // Act: 25/10/2026 é domingo.
        FaturaRegistrada fatura = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 10));

        // Assert
        assertThat(fatura.dataVencimentoEfetiva()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void vencimentoNoNatalNumaSextaTemVencimentoEfetivoNaSegunda() {
        // Act: 25/12/2026 é sexta-feira e feriado nacional; sábado e domingo também não são úteis.
        FaturaRegistrada fatura = servico.faturaDaCompra(cmt, LocalDate.of(2026, 12, 10));

        // Assert: o mês de referência continua o do vencimento nominal (RF14).
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 12));
        assertThat(fatura.dataVencimento()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(fatura.dataVencimentoEfetiva()).isEqualTo(LocalDate.of(2026, 12, 28));
    }

    @Test
    void vencimentoNumFeriadoLocalTemVencimentoEfetivoNoDiaUtilSeguinte() {
        // Arrange: 25/11/2026 é quarta-feira.
        feriados.cadastrarLocalSeAusente(LocalDate.of(2026, 11, 25), "Feriado local de teste");

        // Act
        FaturaRegistrada fatura = servico.faturaDaCompra(amt, LocalDate.of(2026, 11, 10));

        // Assert
        assertThat(fatura.dataVencimento()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(fatura.dataVencimentoEfetiva()).isEqualTo(LocalDate.of(2026, 11, 26));
    }

    @Test
    void vencimentoEmDiaUtilEhOProprioVencimentoEfetivo() {
        // Act: compra em 10/03/2026; vencimento em 25/03/2026, quarta-feira.
        FaturaRegistrada fatura = servico.faturaDaCompra(amt, LocalDate.of(2026, 3, 10));

        // Assert
        assertThat(fatura.dataVencimentoEfetiva()).isEqualTo(fatura.dataVencimento());
    }

    @Test
    void faturaNasceAberta() {
        // Act
        FaturaRegistrada fatura = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 10));

        // Assert: no MVP 1 a fatura não sai de ABERTA (a revisão é do RF15).
        assertThat(fatura.status()).isEqualTo(StatusFatura.ABERTA);
        String statusNoBanco = jdbc.sql("SELECT status FROM fatura WHERE id = ?")
                .param(fatura.id())
                .query(String.class)
                .single();
        assertThat(statusNoBanco).isEqualTo("ABERTA");
    }

    @Test
    void comprasDoMesmoCicloReaproveitamAMesmaFatura() {
        // Arrange
        FaturaRegistrada primeira = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 1));

        // Act: a compra no dia do fechamento, com a regra PROXIMA, vai para a fatura seguinte.
        FaturaRegistrada mesmoCiclo = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 19));
        FaturaRegistrada cicloSeguinte = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 20));

        // Assert
        assertThat(mesmoCiclo).isEqualTo(primeira);
        assertThat(cicloSeguinte.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(contarFaturasDoCartao(amt)).isEqualTo(2);
    }

    @Test
    void cartoesDiferentesTemFaturasDiferentesNoMesmoMes() {
        // Act
        FaturaRegistrada doAmt = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 10));
        FaturaRegistrada doCmt = servico.faturaDaCompra(cmt, LocalDate.of(2026, 10, 10));

        // Assert
        assertThat(doAmt.mesReferencia()).isEqualTo(doCmt.mesReferencia());
        assertThat(doAmt.id()).isNotEqualTo(doCmt.id());
    }

    @Test
    void cartaoInexistenteFalha() {
        // Act e Assert
        assertThatThrownBy(() -> servico.faturaDaCompra(-1L, LocalDate.of(2026, 10, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    // ----- fatura de um mês de referência (parcelas, RF12) -----

    @Test
    void criaAFaturaDeUmMesDeReferenciaComVencimentoNominalEEfetivo() {
        // Act: cartão que fecha no dia 20 e vence no dia 25; 25/10/2026 é domingo.
        FaturaRegistrada fatura = servico.faturaDoMes(amt, YearMonth.of(2026, 10));

        // Assert
        assertThat(fatura.cartaoId()).isEqualTo(amt);
        assertThat(fatura.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(fatura.dataFechamento()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(fatura.dataVencimento()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(fatura.dataVencimentoEfetiva()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(fatura.status()).isEqualTo(StatusFatura.ABERTA);
    }

    @Test
    void faturaDoMesReaproveitaAFaturaCriadaPorUmaCompraDaqueleCiclo() {
        // Arrange
        FaturaRegistrada daCompra = servico.faturaDaCompra(amt, LocalDate.of(2026, 10, 10));

        // Act
        FaturaRegistrada doMes = servico.faturaDoMes(amt, YearMonth.of(2026, 10));

        // Assert
        assertThat(doMes).isEqualTo(daCompra);
        assertThat(contarFaturasDoCartao(amt)).isEqualTo(1);
    }

    @Test
    void faturaDoMesDeCartaoInexistenteFalha() {
        // Act e Assert
        assertThatThrownBy(() -> servico.faturaDoMes(-1L, YearMonth.of(2026, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    /**
     * A corrida: duas compras simultâneas no mesmo cartão e ciclo. As duas procuram a fatura, não
     * a encontram, e as duas tentam criá-la. O teste força essa ordem dos acontecimentos:
     * <ol>
     *   <li>a "minha" transação lê a tabela de faturas e fica com a fotografia em que a fatura não
     *       existe (no MySQL, uma transação REPEATABLE READ continua vendo essa fotografia);</li>
     *   <li>a "outra" compra, em outra thread e outra transação, cria a fatura e confirma;</li>
     *   <li>a minha compra procura a fatura, não a vê, e tenta criá-la: o UNIQUE recusa.</li>
     * </ol>
     * O serviço tem de tratar a recusa e devolver a fatura criada pela outra compra.
     */
    @Test
    void naCorridaOUniqueVenceEOServicoReaproveitaAFaturaCriadaPelaOutraCompra() throws Exception {
        // Arrange
        LocalDate dataCompra = LocalDate.of(2026, 10, 10);
        TransactionTemplate minhaTransacao = new TransactionTemplate(gerenciadorDeTransacoes);
        ExecutorService outraThread = Executors.newSingleThreadExecutor();

        try {
            // Act
            FaturaRegistrada[] resultado = new FaturaRegistrada[2];
            minhaTransacao.executeWithoutResult(status -> {
                int faturasNaFotografia = contarFaturasDoCartao(amt);
                resultado[0] = criarNaOutraThread(outraThread, dataCompra);
                resultado[1] = servico.faturaDaCompra(amt, dataCompra);
                assertThat(faturasNaFotografia).isZero();
            });
            FaturaRegistrada daOutraCompra = resultado[0];
            FaturaRegistrada daMinhaCompra = resultado[1];

            // Assert
            assertThat(daMinhaCompra).isEqualTo(daOutraCompra);
            assertThat(contarFaturasDoCartao(amt)).isEqualTo(1);
        } finally {
            outraThread.shutdown();
        }
    }

    private FaturaRegistrada criarNaOutraThread(ExecutorService outraThread, LocalDate dataCompra) {
        try {
            return outraThread.submit(() -> servico.faturaDaCompra(amt, dataCompra))
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception erro) {
            throw new IllegalStateException("A outra compra falhou.", erro);
        }
    }

    private int contarFaturasDoCartao(long cartaoId) {
        return jdbc.sql("SELECT COUNT(*) FROM fatura WHERE cartao_id = ?")
                .param(cartaoId)
                .query(Integer.class)
                .single();
    }
}
