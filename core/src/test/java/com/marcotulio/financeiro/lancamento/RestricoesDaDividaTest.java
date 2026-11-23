package com.marcotulio.financeiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restrições da tabela {@code divida} e das colunas de parcela em {@code lancamento} (bloco B de
 * {@code docs/design/modelo-dados.md}, seções 3.5 e 3.7) garantidas pelo próprio banco.
 *
 * <p>Como no {@code RestricoesDoLancamentoTest}, os testes gravam com SQL direto, sem passar
 * pelas entidades, para provar que a regra está no schema. {@code @Transactional} desfaz cada
 * teste ao final.
 */
@Transactional
class RestricoesDaDividaTest extends TesteDeIntegracao {

    @Autowired
    private JdbcClient jdbc;

    private long categoria;
    private long destinatario;
    private long cartao;

    @BeforeEach
    void cadastraOQueUmaDividaReferencia() {
        jdbc.sql("INSERT INTO destinatario (nome, marcacao_bot, %s) VALUES ('Visitante', '@vis', %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .update();
        destinatario = ultimoIdGerado();
        jdbc.sql("INSERT INTO categoria (nome, tipo, %s) VALUES ('Categoria de teste', 'DESPESA', %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .update();
        categoria = ultimoIdGerado();
        jdbc.sql("""
                        INSERT INTO usuario (nome, login, senha_hash, destinatario_id, %s)
                        VALUES ('Visitante', 'visitante', '{bcrypt}hash-de-teste', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(destinatario)
                .update();
        long titular = ultimoIdGerado();
        jdbc.sql("""
                        INSERT INTO cartao (apelido, nome, titular_id, dia_fechamento, dia_vencimento,
                                            compra_no_fechamento, %s)
                        VALUES ('tst', 'Banco B', ?, 5, 12, 'PROXIMA', %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(titular)
                .update();
        cartao = ultimoIdGerado();
    }

    // ----- divida -----

    @Test
    void aceitaUmParcelamentoDeCartaoAtivo() {
        // Act
        long divida = inserirDivida(new Divida());

        // Assert
        var gravada = jdbc.sql("SELECT valor_total, quantidade_parcelas, status FROM divida WHERE id = ?")
                .param(divida)
                .query((rs, linha) -> rs.getBigDecimal(1) + "|" + rs.getInt(2) + "|" + rs.getString(3))
                .single();
        assertThat(gravada).isEqualTo("1698.88|16|ATIVA");
    }

    @Test
    void tipoSoAceitaParcelamentoDeCartaoFinanciamentoOuEmprestimo() {
        // Arrange: os três tipos do modelo são aceitos.
        inserirDivida(new Divida().tipo("PARCELAMENTO_CARTAO"));
        inserirDivida(new Divida().tipo("FINANCIAMENTO").semCartao());
        inserirDivida(new Divida().tipo("EMPRESTIMO").semCartao());

        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().tipo("CONSORCIO")))
                .satisfies(violou("ck_divida_tipo"));
    }

    @Test
    void valorTotalPrecisaSerPositivo() {
        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().valorTotal("0.00")))
                .satisfies(violou("ck_divida_valor_total_positivo"));
    }

    @Test
    void temPeloMenosDuasParcelas() {
        // Arrange: duas parcelas é o mínimo aceito.
        inserirDivida(new Divida().quantidadeParcelas(2));

        // Act e Assert: uma "dívida" de uma parcela é uma compra à vista.
        assertThatThrownBy(() -> inserirDivida(new Divida().quantidadeParcelas(1)))
                .satisfies(violou("ck_divida_quantidade_parcelas"));
    }

    @Test
    void parcelamentoDeCartaoExigeOCartaoEOsDemaisTiposNao() {
        // Arrange: um financiamento pode ser pago no cartão ou fora dele.
        inserirDivida(new Divida().tipo("FINANCIAMENTO"));
        inserirDivida(new Divida().tipo("FINANCIAMENTO").semCartao());

        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().semCartao()))
                .satisfies(violou("ck_divida_parcelamento_exige_cartao"));
    }

    @Test
    void statusSoAceitaAtivaOuQuitada() {
        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().status("PAGA")))
                .satisfies(violou("ck_divida_status"));
    }

    @Test
    void dataDeQuitacaoExisteSeESoSeADividaEstaQuitada() {
        // Arrange: os dois casos coerentes são aceitos.
        inserirDivida(new Divida());
        inserirDivida(new Divida().status("QUITADA").dataQuitacao("2027-03-12"));

        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().status("QUITADA")))
                .satisfies(violou("ck_divida_quitada_tem_data"));
        assertThatThrownBy(() -> inserirDivida(new Divida().dataQuitacao("2027-03-12")))
                .satisfies(violou("ck_divida_quitada_tem_data"));
    }

    @Test
    void exigeCartaoEDestinatarioExistentes() {
        // Act e Assert
        assertThatThrownBy(() -> inserirDivida(new Divida().cartao(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserirDivida(new Divida().destinatario(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saldoDevedorNaoEhGravado() {
        // Act: o saldo é calculado das parcelas (modelo de dados, seção 3.5).
        var colunas = jdbc.sql("""
                        SELECT column_name FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND table_name = 'divida'
                        """)
                .query(String.class)
                .list();

        // Assert
        assertThat(colunas).isNotEmpty().noneMatch(coluna -> coluna.toLowerCase().contains("saldo"));
    }

    @Test
    void dividaUsaUtf8mb4ComCollationQueIgnoraAcentos() {
        // Act
        String collation = jdbc.sql("""
                        SELECT table_collation FROM information_schema.tables
                         WHERE table_schema = DATABASE() AND table_name = 'divida'
                        """)
                .query(String.class)
                .single();

        // Assert
        assertThat(collation).isEqualTo("utf8mb4_0900_ai_ci");
    }

    // ----- parcelas em lancamento -----

    @Test
    void lancamentoAvulsoNaoTemDividaNemNumeroDeParcela() {
        // Act
        long avulso = inserirLancamento(null, null);

        // Assert
        var gravado = jdbc.sql("SELECT divida_id, numero_parcela FROM lancamento WHERE id = ?")
                .param(avulso)
                .query((rs, linha) -> new Object[] {rs.getObject(1), rs.getObject(2)})
                .single();
        assertThat(gravado).containsOnlyNulls();
    }

    @Test
    void dividaENumeroDaParcelaSaoPreenchidosJuntos() {
        // Arrange
        long divida = inserirDivida(new Divida());
        inserirLancamento(divida, 1);

        // Act e Assert
        assertThatThrownBy(() -> inserirLancamento(divida, null))
                .satisfies(violou("ck_lancamento_divida_e_parcela_juntas"));
        assertThatThrownBy(() -> inserirLancamento(null, 2))
                .satisfies(violou("ck_lancamento_divida_e_parcela_juntas"));
    }

    @Test
    void numeroDaParcelaComecaEm1() {
        // Arrange
        long divida = inserirDivida(new Divida());

        // Act e Assert
        assertThatThrownBy(() -> inserirLancamento(divida, 0))
                .satisfies(violou("ck_lancamento_numero_parcela"));
    }

    @Test
    void naoHaDuasParcelasComOMesmoNumeroNaMesmaDivida() {
        // Arrange: o mesmo número em dívidas diferentes é aceito.
        long celular = inserirDivida(new Divida());
        long geladeira = inserirDivida(new Divida());
        inserirLancamento(celular, 1);
        inserirLancamento(geladeira, 1);

        // Act e Assert
        assertThatThrownBy(() -> inserirLancamento(celular, 1))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_lancamento_divida_parcela");
    }

    @Test
    void parcelaExigeDividaExistente() {
        // Act e Assert
        assertThatThrownBy(() -> inserirLancamento(-1L, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- auxiliares -----

    /** Ver o {@code RestricoesDoLancamentoTest}: o erro de {@code CHECK} é conferido pelo nome. */
    private static ThrowingConsumer<Throwable> violou(String restricao) {
        return erro -> assertThat(erro)
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint '" + restricao + "' is violated");
    }

    /**
     * Uma linha válida de {@code divida} (o celular do ERS: 16 × R$ 106,18 no Banco B); cada teste
     * muda só o campo que quer provar.
     */
    private final class Divida {
        private String tipo = "PARCELAMENTO_CARTAO";
        private String valorTotal = "1698.88";
        private int quantidadeParcelas = 16;
        private Long cartaoId = cartao;
        private Long destinatarioId = destinatario;
        private String status = "ATIVA";
        private String dataQuitacao;

        Divida tipo(String novo) {
            tipo = novo;
            return this;
        }

        Divida valorTotal(String novo) {
            valorTotal = novo;
            return this;
        }

        Divida quantidadeParcelas(int nova) {
            quantidadeParcelas = nova;
            return this;
        }

        Divida cartao(long novo) {
            cartaoId = novo;
            return this;
        }

        Divida semCartao() {
            cartaoId = null;
            return this;
        }

        Divida destinatario(long novo) {
            destinatarioId = novo;
            return this;
        }

        Divida status(String novo) {
            status = novo;
            return this;
        }

        Divida dataQuitacao(String nova) {
            dataQuitacao = nova;
            return this;
        }
    }

    private long inserirDivida(Divida divida) {
        jdbc.sql("""
                        INSERT INTO divida (tipo, descricao, valor_total, quantidade_parcelas, cartao_id,
                                            destinatario_id, data_contratacao, status, data_quitacao, %s)
                        VALUES (?, 'celular', ?, ?, ?, ?, '2026-10-10', ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(divida.tipo, divida.valorTotal, divida.quantidadeParcelas, divida.cartaoId,
                        divida.destinatarioId, divida.status, divida.dataQuitacao)
                .update();
        return ultimoIdGerado();
    }

    private long inserirLancamento(Long dividaId, Integer numeroParcela) {
        jdbc.sql("""
                        INSERT INTO lancamento (tipo, valor, descricao, categoria_id, destinatario_id,
                                                data_compra, mes_referencia, origem, divida_id,
                                                numero_parcela, %s)
                        VALUES ('DESPESA', '106.18', 'celular 1/16', ?, ?, '2026-10-10', '2026-11-01',
                                'WEB', ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(categoria, destinatario, dividaId, numeroParcela)
                .update();
        return ultimoIdGerado();
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
