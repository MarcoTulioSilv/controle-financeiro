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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restrições da tabela {@code lancamento} (blocos A e B de {@code docs/design/modelo-dados.md})
 * garantidas pelo próprio banco.
 *
 * <p>Os testes gravam com SQL direto, sem passar pela entidade, para provar que a regra está no
 * schema. {@code @Transactional} desfaz cada teste ao final.
 */
@Transactional
class RestricoesDoLancamentoTest extends TesteDeIntegracao {

    @Autowired
    private JdbcClient jdbc;

    private long categoria;
    private long destinatario;
    private long cartao;
    private long fatura;

    @BeforeEach
    void cadastraOQueUmLancamentoReferencia() {
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
                        VALUES ('tst', 'Banco A', ?, 25, 5, 'PROXIMA', %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(titular)
                .update();
        cartao = ultimoIdGerado();
        jdbc.sql("""
                        INSERT INTO fatura (cartao_id, mes_referencia, data_fechamento, data_vencimento,
                                            data_vencimento_efetiva, status, %s)
                        VALUES (?, '2026-10-01', '2026-09-25', '2026-10-05', '2026-10-05', 'ABERTA', %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(cartao)
                .update();
        fatura = ultimoIdGerado();
    }

    @Test
    void aceitaUmLancamentoValidoNaoMarcadoParaRevisarENaoExcluido() {
        // Act
        long lancamento = inserir(new Linha());

        // Assert
        var gravado = jdbc.sql("SELECT valor, revisar, excluido_em FROM lancamento WHERE id = ?")
                .param(lancamento)
                .query((rs, linha) -> new Object[] {rs.getBigDecimal(1), rs.getBoolean(2), rs.getObject(3)})
                .single();
        assertThat(gravado[0]).hasToString("150.00");
        assertThat(gravado[1]).isEqualTo(false);
        assertThat(gravado[2]).isNull();
    }

    @Test
    void valorPrecisaSerPositivo() {
        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().valor("0.00")))
                .satisfies(violou("ck_lancamento_valor_positivo"));
        assertThatThrownBy(() -> inserir(new Linha().valor("-0.01")))
                .satisfies(violou("ck_lancamento_valor_positivo"));
    }

    @Test
    void tipoSoAceitaReceitaOuDespesa() {
        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().tipo("AJUSTE")))
                .satisfies(violou("ck_lancamento_tipo"));
    }

    @Test
    void origemSoAceitaWebBotOuImportacao() {
        // Arrange: as três origens do modelo são aceitas.
        inserir(new Linha().origem("WEB"));
        inserir(new Linha().origem("BOT"));
        inserir(new Linha().origem("IMPORTACAO"));

        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().origem("PLANILHA")))
                .satisfies(violou("ck_lancamento_origem"));
    }

    @Test
    void mesDeReferenciaEhSempreODia1() {
        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().mesReferencia("2026-10-05")))
                .satisfies(violou("ck_lancamento_mes_referencia_dia_1"));
    }

    @Test
    void faturaExigeCartao() {
        // Arrange: com o cartão, a fatura é aceita.
        inserir(new Linha().cartao(cartao).fatura(fatura));

        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().fatura(fatura)))
                .satisfies(violou("ck_lancamento_fatura_exige_cartao"));
    }

    @Test
    void cartaoSemFaturaEhAceito() {
        // Act: é o caso do mês de referência informado diferente do da fatura (ServicoLancamentos).
        long lancamento = inserir(new Linha().cartao(cartao));

        // Assert
        assertThat(lancamento).isPositive();
    }

    @Test
    void exigeCategoriaDestinatarioCartaoEFaturaExistentes() {
        // Act e Assert
        assertThatThrownBy(() -> inserir(new Linha().categoria(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserir(new Linha().destinatario(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserir(new Linha().cartao(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserir(new Linha().cartao(cartao).fatura(-1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void temOsIndicesDoResumoMensalEDoPlanejadoPorCategoria() {
        // Act
        var indices = jdbc.sql("""
                        SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS colunas
                          FROM information_schema.statistics
                         WHERE table_schema = DATABASE() AND table_name = 'lancamento'
                           AND index_name IN ('idx_lancamento_mes', 'idx_lancamento_categoria_mes')
                         GROUP BY index_name
                        """)
                .query((rs, linha) -> rs.getString(1) + "=" + rs.getString(2))
                .list();

        // Assert
        assertThat(indices).containsExactlyInAnyOrder(
                "idx_lancamento_mes=mes_referencia",
                "idx_lancamento_categoria_mes=categoria_id,mes_referencia");
    }

    @Test
    void usaUtf8mb4ComCollationQueIgnoraAcentos() {
        // Act
        String collation = jdbc.sql("""
                        SELECT table_collation FROM information_schema.tables
                         WHERE table_schema = DATABASE() AND table_name = 'lancamento'
                        """)
                .query(String.class)
                .single();

        // Assert
        assertThat(collation).isEqualTo("utf8mb4_0900_ai_ci");
    }

    // ----- auxiliares -----

    /**
     * O MySQL recusou a gravação por causa do {@code CHECK} indicado. O Spring não classifica o
     * erro de {@code CHECK} do MySQL (código 3819) como {@link DataIntegrityViolationException},
     * então o teste confere o nome da restrição na mensagem.
     */
    private static ThrowingConsumer<Throwable> violou(String restricao) {
        return erro -> assertThat(erro)
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint '" + restricao + "' is violated");
    }

    /** Uma linha válida de {@code lancamento}; cada teste muda só o campo que quer provar. */
    private final class Linha {
        private String tipo = "DESPESA";
        private String valor = "150.00";
        private Long categoriaId = categoria;
        private Long destinatarioId = destinatario;
        private String mesReferencia = "2026-10-01";
        private String origem = "WEB";
        private Long cartaoId;
        private Long faturaId;

        Linha tipo(String novo) {
            tipo = novo;
            return this;
        }

        Linha valor(String novo) {
            valor = novo;
            return this;
        }

        Linha categoria(long nova) {
            categoriaId = nova;
            return this;
        }

        Linha destinatario(long novo) {
            destinatarioId = novo;
            return this;
        }

        Linha mesReferencia(String novo) {
            mesReferencia = novo;
            return this;
        }

        Linha origem(String nova) {
            origem = nova;
            return this;
        }

        Linha cartao(long novo) {
            cartaoId = novo;
            return this;
        }

        Linha fatura(long nova) {
            faturaId = nova;
            return this;
        }
    }

    private long inserir(Linha linha) {
        jdbc.sql("""
                        INSERT INTO lancamento (tipo, valor, descricao, categoria_id, destinatario_id,
                                                data_compra, mes_referencia, origem, cartao_id,
                                                fatura_id, %s)
                        VALUES (?, ?, 'Mercado', ?, ?, '2026-10-01', ?, ?, ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(linha.tipo, linha.valor, linha.categoriaId, linha.destinatarioId,
                        linha.mesReferencia, linha.origem, linha.cartaoId, linha.faturaId)
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
