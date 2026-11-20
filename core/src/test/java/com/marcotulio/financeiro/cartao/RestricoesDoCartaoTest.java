package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restrições das tabelas {@code cartao}, {@code fatura} e {@code feriado} (bloco B de
 * {@code docs/design/modelo-dados.md}) garantidas pelo próprio banco.
 *
 * <p>Os testes gravam com SQL direto, sem passar pelas entidades, para provar que a regra está no
 * schema. {@code @Transactional} desfaz cada teste ao final.
 */
@Transactional
class RestricoesDoCartaoTest extends TesteDeIntegracao {

    @Autowired
    private JdbcClient jdbc;

    private long titular;

    @BeforeEach
    void cadastraOTitular() {
        jdbc.sql("INSERT INTO destinatario (nome, marcacao_bot, %s) VALUES ('Visitante', '@vis', %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .update();
        long destinatario = ultimoIdGerado();
        jdbc.sql("""
                        INSERT INTO usuario (nome, login, senha_hash, destinatario_id, %s)
                        VALUES ('Visitante', 'visitante', '{bcrypt}hash-de-teste', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(destinatario)
                .update();
        titular = ultimoIdGerado();
    }

    // ----- cartao -----

    @Test
    void cartaoNasceAtivo() {
        // Arrange
        long cartao = inserirCartao("tst", 25, 5, "PROXIMA");

        // Act
        Boolean ativo = jdbc.sql("SELECT ativo FROM cartao WHERE id = ?")
                .param(cartao)
                .query(Boolean.class)
                .single();

        // Assert
        assertThat(ativo).isTrue();
    }

    @Test
    void cartaoNaoAceitaApelidoRepetidoNemComOutraCaixa() {
        // Arrange
        inserirCartao("tst", 25, 5, "PROXIMA");

        // Act e Assert: pela collation, TST e tst são o mesmo apelido.
        assertThatThrownBy(() -> inserirCartao("TST", 10, 15, "ATUAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cartaoNaoAceitaDiaDeFechamentoForaDe1A31() {
        // Act e Assert
        assertThatThrownBy(() -> inserirCartao("tst", 0, 5, "PROXIMA"))
                .satisfies(violou("ck_cartao_dia_fechamento"));
        assertThatThrownBy(() -> inserirCartao("tst", 32, 5, "PROXIMA"))
                .satisfies(violou("ck_cartao_dia_fechamento"));
    }

    @Test
    void cartaoNaoAceitaDiaDeVencimentoForaDe1A31() {
        // Act e Assert
        assertThatThrownBy(() -> inserirCartao("tst", 25, 0, "PROXIMA"))
                .satisfies(violou("ck_cartao_dia_vencimento"));
        assertThatThrownBy(() -> inserirCartao("tst", 25, 32, "PROXIMA"))
                .satisfies(violou("ck_cartao_dia_vencimento"));
    }

    @Test
    void cartaoSoAceitaCompraNoFechamentoAtualOuProxima() {
        // Act e Assert
        assertThatThrownBy(() -> inserirCartao("tst", 25, 5, "ANTERIOR"))
                .satisfies(violou("ck_cartao_compra_no_fechamento"));
    }

    @Test
    void cartaoExigeTitularExistente() {
        // Arrange
        titular = -1L;

        // Act e Assert
        assertThatThrownBy(() -> inserirCartao("tst", 25, 5, "PROXIMA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- fatura -----

    @Test
    void faturaNaoSeRepeteParaOMesmoCartaoEMes() {
        // Arrange
        long cartao = inserirCartao("tst", 25, 5, "PROXIMA");
        inserirFatura(cartao, "2026-10-01", "ABERTA");

        // Act e Assert: é este UNIQUE que resolve a corrida entre duas compras simultâneas.
        assertThatThrownBy(() -> inserirFatura(cartao, "2026-10-01", "ABERTA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cartoesDiferentesTemFaturasDoMesmoMes() {
        // Arrange
        long umCartao = inserirCartao("tst", 25, 5, "PROXIMA");
        long outroCartao = inserirCartao("tst2", 25, 5, "PROXIMA");
        inserirFatura(umCartao, "2026-10-01", "ABERTA");

        // Act
        inserirFatura(outroCartao, "2026-10-01", "ABERTA");

        // Assert
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fatura WHERE mes_referencia = '2026-10-01'")
                .query(Integer.class)
                .single();
        assertThat(total).isEqualTo(2);
    }

    @Test
    void mesDeReferenciaDaFaturaEhSempreODia1() {
        // Arrange
        long cartao = inserirCartao("tst", 25, 5, "PROXIMA");

        // Act e Assert
        assertThatThrownBy(() -> inserirFatura(cartao, "2026-10-05", "ABERTA"))
                .satisfies(violou("ck_fatura_mes_referencia_dia_1"));
    }

    @Test
    void faturaSoAceitaOsStatusDoModelo() {
        // Arrange
        long cartao = inserirCartao("tst", 25, 5, "PROXIMA");

        // Act e Assert
        assertThatThrownBy(() -> inserirFatura(cartao, "2026-10-01", "FECHADA"))
                .satisfies(violou("ck_fatura_status"));
    }

    @Test
    void faturaExigeCartaoExistente() {
        // Act e Assert
        assertThatThrownBy(() -> inserirFatura(-1L, "2026-10-01", "ABERTA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- feriado -----

    @Test
    void feriadoNaoSeRepeteNaMesmaData() {
        // Arrange
        inserirFeriado("2030-07-15", "LOCAL");

        // Act e Assert
        assertThatThrownBy(() -> inserirFeriado("2030-07-15", "NACIONAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void feriadoSoAceitaAbrangenciaNacionalOuLocal() {
        // Act e Assert
        assertThatThrownBy(() -> inserirFeriado("2030-07-15", "ESTADUAL"))
                .satisfies(violou("ck_feriado_abrangencia"));
    }

    // ----- charset e collation (C3) -----

    @Test
    void asTabelasDoCartaoUsamUtf8mb4ComCollationQueIgnoraAcentos() {
        // Act
        var collations = jdbc.sql("""
                        SELECT table_collation FROM information_schema.tables
                         WHERE table_schema = DATABASE()
                           AND table_name IN ('cartao', 'fatura', 'feriado')
                        """)
                .query(String.class)
                .list();

        // Assert
        assertThat(collations).hasSize(3).containsOnly("utf8mb4_0900_ai_ci");
    }

    // ----- auxiliares -----

    /**
     * O MySQL recusou a gravação por causa do {@code CHECK} indicado. O Spring não classifica o
     * erro de {@code CHECK} do MySQL (código 3819) como {@link DataIntegrityViolationException},
     * então o teste confere o nome da restrição na mensagem: assim também prova que foi ela, e não
     * outra, que barrou o dado.
     */
    private static ThrowingConsumer<Throwable> violou(String restricao) {
        return erro -> assertThat(erro)
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint '" + restricao + "' is violated");
    }

    private long inserirCartao(String apelido, int diaFechamento, int diaVencimento, String compraNoFechamento) {
        jdbc.sql("""
                        INSERT INTO cartao (apelido, nome, titular_id, dia_fechamento, dia_vencimento,
                                            compra_no_fechamento, %s)
                        VALUES (?, 'Banco A', ?, ?, ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(apelido, titular, diaFechamento, diaVencimento, compraNoFechamento)
                .update();
        return ultimoIdGerado();
    }

    private void inserirFatura(long cartao, String mesReferencia, String status) {
        jdbc.sql("""
                        INSERT INTO fatura (cartao_id, mes_referencia, data_fechamento, data_vencimento,
                                            data_vencimento_efetiva, status, %s)
                        VALUES (?, ?, '2026-09-25', '2026-10-05', '2026-10-05', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(cartao, mesReferencia, status)
                .update();
    }

    private void inserirFeriado(String data, String abrangencia) {
        jdbc.sql("""
                        INSERT INTO feriado (data, descricao, abrangencia, %s)
                        VALUES (?, 'Feriado de teste', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(data, abrangencia)
                .update();
    }

    private long ultimoIdGerado() {
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    /** Colunas padrão de auditoria e versão (C7), que todo INSERT precisa preencher. */
    private static final String COLUNAS_C7 =
            "criado_em, criado_por, atualizado_em, atualizado_por, versao";

    private static final String VALORES_C7 =
            "UTC_TIMESTAMP(3), 'teste', UTC_TIMESTAMP(3), 'teste', 0";
}
