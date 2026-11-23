package com.marcotulio.financeiro.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Visões SQL lidas pelo Power BI Desktop (ADR-007): a regra de cálculo está nelas, e estes
 * testes a conferem contra resultados calculados à mão, com dados fictícios do ERS.
 *
 * <p>Os dados são gravados com SQL direto porque o teste precisa de lançamentos com exclusão
 * lógica ({@code excluido_em} preenchido), que nenhum serviço do MVP 1 produz; a T16 vai
 * produzi-los ao abater parcelas. {@code @Transactional} desfaz cada teste ao final.
 *
 * <p>Os meses fixos (jul a out/2026) já passaram; 2099 é sempre um mês futuro.
 */
@Transactional
class VisoesParaRelatoriosTest extends TesteDeIntegracao {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private JdbcClient jdbc;

    private long trabalhoA;
    private long vendaDeAtivos;
    private long alimentacao;
    private long lazer;
    private long compraDeAtivos;

    private long marco;
    private long ana;
    private long casa;

    /** Banco A do Marco (ERS, Apêndice B). */
    private long amt;

    @BeforeEach
    void cadastraCategoriasDestinatariosECartao() {
        trabalhoA = inserirCategoria("Trabalho A", "RECEITA");
        vendaDeAtivos = inserirCategoria("Investimentos", "RECEITA");
        alimentacao = inserirCategoria("Alimentação", "DESPESA");
        lazer = inserirCategoria("Lazer", "DESPESA");
        compraDeAtivos = inserirCategoria("Investimentos", "DESPESA");

        marco = inserirDestinatario("Marco", "@mt");
        ana = inserirDestinatario("Ana", "@ana");
        casa = inserirDestinatario("Casa", "@casa");

        jdbc.sql("""
                        INSERT INTO usuario (nome, login, senha_hash, destinatario_id, %s)
                        VALUES ('Marco', 'marco', '{bcrypt}hash-de-teste', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(marco)
                .update();
        long titular = ultimoIdGerado();
        jdbc.sql("""
                        INSERT INTO cartao (apelido, nome, titular_id, dia_fechamento, dia_vencimento,
                                            compra_no_fechamento, %s)
                        VALUES ('amt', 'Banco A', ?, 20, 25, 'PROXIMA', %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .param(titular)
                .update();
        amt = ultimoIdGerado();
    }

    // ----- visao_lancamento -----

    @Test
    void visaoDeLancamentosTrazOsNomesNoLugarDosIds() {
        // Arrange
        new Lancamento("DESPESA", "150.00", alimentacao, casa, "2026-10-01")
                .descricao("Mercado").dataCompra("2026-09-22").cartao(amt).inserir();
        new Lancamento("RECEITA", "5000.00", trabalhoA, marco, "2026-10-01")
                .descricao("Salário").dataCompra("2026-10-05").inserir();

        // Act
        List<String> linhas = jdbc.sql("""
                        SELECT tipo, valor, valor_com_sinal, descricao, data_compra, mes_referencia,
                               categoria, para_quem, cartao, cartao_nome, origem
                          FROM visao_lancamento
                         ORDER BY data_compra
                        """)
                .query((rs, n) -> String.join(" | ",
                        rs.getString("tipo"), rs.getString("valor"), rs.getString("valor_com_sinal"),
                        rs.getString("descricao"), rs.getString("data_compra"),
                        rs.getString("mes_referencia"), rs.getString("categoria"),
                        rs.getString("para_quem"), String.valueOf(rs.getString("cartao")),
                        String.valueOf(rs.getString("cartao_nome")), rs.getString("origem")))
                .list();

        // Assert: a despesa entra com sinal negativo; o lançamento sem cartão não some (LEFT JOIN).
        assertThat(linhas).containsExactly(
                "DESPESA | 150.00 | -150.00 | Mercado | 2026-09-22 | 2026-10-01 | Alimentação | Casa"
                        + " | amt | Banco A | WEB",
                "RECEITA | 5000.00 | 5000.00 | Salário | 2026-10-05 | 2026-10-01 | Trabalho A | Marco"
                        + " | null | null | WEB");
    }

    @Test
    void visaoDeLancamentosIgnoraOsExcluidosLogicamente() {
        // Arrange
        new Lancamento("DESPESA", "150.00", alimentacao, casa, "2026-10-01").inserir();
        new Lancamento("DESPESA", "999.99", alimentacao, casa, "2026-10-01").excluido().inserir();

        // Act
        List<String> valores = jdbc.sql("SELECT valor FROM visao_lancamento")
                .query(String.class)
                .list();

        // Assert
        assertThat(valores).containsExactly("150.00");
    }

    // ----- visao_resumo_mensal -----

    @Test
    void resumoMensalCalculaReceitasDespesasResultadoEEconomia() {
        // Arrange: ago/2026 tem uma compra de ativo; set/2026, uma venda; out/2026, nenhuma receita.
        new Lancamento("RECEITA", "5000.00", trabalhoA, marco, "2026-08-01").inserir();
        new Lancamento("DESPESA", "1200.00", alimentacao, casa, "2026-08-01").inserir();
        new Lancamento("DESPESA", "300.00", lazer, ana, "2026-08-01").inserir();
        new Lancamento("DESPESA", "500.00", compraDeAtivos, marco, "2026-08-01").inserir();
        new Lancamento("DESPESA", "999.99", alimentacao, casa, "2026-08-01").excluido().inserir();

        new Lancamento("RECEITA", "5000.00", trabalhoA, marco, "2026-09-01").inserir();
        new Lancamento("RECEITA", "1000.00", vendaDeAtivos, marco, "2026-09-01").inserir();
        new Lancamento("DESPESA", "1500.00", alimentacao, casa, "2026-09-01").inserir();
        new Lancamento("DESPESA", "4800.00", lazer, ana, "2026-09-01").inserir();
        new Lancamento("RECEITA", "777.77", trabalhoA, marco, "2026-09-01").excluido().inserir();

        new Lancamento("DESPESA", "100.00", alimentacao, casa, "2026-10-01").inserir();

        new Lancamento("DESPESA", "50.00", lazer, ana, "2099-01-01").inserir();

        inserirMeta("2026-01-01", "10.00");
        inserirMeta("2026-09-01", "15.00");

        // Act
        List<String> resumo = jdbc.sql("""
                        SELECT mes_referencia, receitas, despesas, resultado, compras_ativos,
                               vendas_ativos, percentual_economia, meta_economia
                          FROM visao_resumo_mensal
                         ORDER BY mes_referencia
                        """)
                .query((rs, n) -> String.join(" | ",
                        rs.getString("mes_referencia"), rs.getString("receitas"),
                        rs.getString("despesas"), rs.getString("resultado"),
                        rs.getString("compras_ativos"), rs.getString("vendas_ativos"),
                        String.valueOf(rs.getString("percentual_economia")),
                        String.valueOf(rs.getString("meta_economia"))))
                .list();

        // Assert, calculado à mão pela fórmula do RF04,
        // (resultado + compras de ativos − vendas de ativos) ÷ receitas:
        //   ago: (3000,00 + 500,00 − 0)   ÷ 5000,00 = 70,00%
        //   set: (−300,00 + 0 − 1000,00) ÷ 6000,00 = −21,666...% → −21,67%
        //   out e jan/2099: sem receita, a economia não é calculável (nula).
        // Os excluídos (999,99 e 777,77) não entram. A meta segue a vigência: 10% até ago, 15% depois.
        assertThat(resumo).containsExactly(
                "2026-08-01 | 5000.00 | 2000.00 | 3000.00 | 500.00 | 0.00 | 70.00 | 10.00",
                "2026-09-01 | 6000.00 | 6300.00 | -300.00 | 0.00 | 1000.00 | -21.67 | 15.00",
                "2026-10-01 | 0.00 | 100.00 | -100.00 | 0.00 | 0.00 | null | 15.00",
                "2099-01-01 | 0.00 | 50.00 | -50.00 | 0.00 | 0.00 | null | 15.00");
    }

    @Test
    void resumoMensalSemMetaVigenteDeixaAMetaNula() {
        // Arrange
        new Lancamento("RECEITA", "1000.00", trabalhoA, marco, "2026-08-01").inserir();
        inserirMeta("2026-09-01", "10.00");

        // Act
        List<String> metas = jdbc.sql("SELECT meta_economia FROM visao_resumo_mensal")
                .query(String.class)
                .list();

        // Assert: o mês aparece, mas a meta só vigora a partir de set/2026.
        assertThat(metas).containsExactly((String) null);
    }

    // ----- visao_planejado_real -----

    @Test
    void planejadoRealPorCategoriaRespeitaAVigenciaDoOrcamento() {
        // Arrange: o orçamento da Alimentação muda em out/2026; o do Trabalho A começa em set/2026.
        inserirOrcamento(alimentacao, "2026-08-01", "1000.00");
        inserirOrcamento(alimentacao, "2026-10-01", "1300.00");
        inserirOrcamento(lazer, "2026-08-01", "400.00");
        inserirOrcamento(trabalhoA, "2026-09-01", "4500.00");

        new Lancamento("DESPESA", "1200.00", alimentacao, casa, "2026-08-01").inserir();
        new Lancamento("DESPESA", "300.00", lazer, ana, "2026-08-01").inserir();
        new Lancamento("DESPESA", "500.00", compraDeAtivos, marco, "2026-08-01").inserir();
        new Lancamento("DESPESA", "1500.00", alimentacao, casa, "2026-09-01").inserir();
        new Lancamento("RECEITA", "5000.00", trabalhoA, marco, "2026-09-01").inserir();
        new Lancamento("DESPESA", "100.00", alimentacao, casa, "2026-10-01").inserir();
        new Lancamento("DESPESA", "999.99", alimentacao, casa, "2026-10-01").excluido().inserir();

        // Act
        List<String> linhas = jdbc.sql("""
                        SELECT mes_referencia, categoria, tipo, valor_planejado, valor_real, diferenca
                          FROM visao_planejado_real
                         ORDER BY mes_referencia, categoria, tipo
                        """)
                .query((rs, n) -> String.join(" | ",
                        rs.getString("mes_referencia"), rs.getString("categoria"), rs.getString("tipo"),
                        rs.getString("valor_planejado"), rs.getString("valor_real"),
                        rs.getString("diferenca")))
                .list();

        // Assert: diferença = planejado − real. Sem orçamento vigente, o planejado é zero; com
        // orçamento e sem lançamento, o real é zero. Em set, a Alimentação ainda usa o de ago.
        assertThat(linhas).containsExactly(
                "2026-08-01 | Alimentação | DESPESA | 1000.00 | 1200.00 | -200.00",
                "2026-08-01 | Investimentos | DESPESA | 0.00 | 500.00 | -500.00",
                "2026-08-01 | Lazer | DESPESA | 400.00 | 300.00 | 100.00",
                "2026-09-01 | Alimentação | DESPESA | 1000.00 | 1500.00 | -500.00",
                "2026-09-01 | Lazer | DESPESA | 400.00 | 0.00 | 400.00",
                "2026-09-01 | Trabalho A | RECEITA | 4500.00 | 5000.00 | -500.00",
                "2026-10-01 | Alimentação | DESPESA | 1300.00 | 100.00 | 1200.00",
                "2026-10-01 | Lazer | DESPESA | 400.00 | 0.00 | 400.00",
                "2026-10-01 | Trabalho A | RECEITA | 4500.00 | 0.00 | 4500.00");
    }

    // ----- visao_reserva_mensal -----

    @Test
    void reservaAcumulaAjustesEResultadosMesAMes() {
        // Arrange
        inserirAjuste("2026-07-31", "2000.00", "saldo antes do sistema");
        inserirAjuste("2026-09-15", "-200.00", "correção conforme extrato");

        new Lancamento("RECEITA", "5000.00", trabalhoA, marco, "2026-08-01").inserir();
        new Lancamento("DESPESA", "2000.00", alimentacao, casa, "2026-08-01").inserir();
        new Lancamento("DESPESA", "999.99", alimentacao, casa, "2026-08-01").excluido().inserir();
        new Lancamento("RECEITA", "6000.00", trabalhoA, marco, "2026-09-01").inserir();
        new Lancamento("DESPESA", "6300.00", lazer, ana, "2026-09-01").inserir();
        new Lancamento("DESPESA", "100.00", alimentacao, casa, "2026-10-01").inserir();

        // Act
        List<String> reserva = reservaAte("2026-12-01");

        // Assert, à mão:
        //   jul: 0       + ajuste 2000,00                       = 2000,00
        //   ago: 2000,00 + resultado 3000,00 (o excluído não entra) = 5000,00
        //   set: 5000,00 − ajuste 200,00 + resultado −300,00     = 4500,00 (déficit diminui)
        //   out: 4500,00 + resultado −100,00                      = 4400,00
        assertThat(reserva).containsExactly(
                "2026-07-01 | 2000.00 | 0.00 | 0.00 | 2000.00",
                "2026-08-01 | 0.00 | 3000.00 | 2000.00 | 5000.00",
                "2026-09-01 | -200.00 | -300.00 | 5000.00 | 4500.00",
                "2026-10-01 | 0.00 | -100.00 | 4500.00 | 4400.00");
    }

    @Test
    void reservaPodeFicarNegativa() {
        // Arrange
        inserirAjuste("2026-07-31", "100.00", "saldo antes do sistema");
        new Lancamento("DESPESA", "350.00", lazer, ana, "2026-08-01").inserir();

        // Act
        List<String> reserva = reservaAte("2026-12-01");

        // Assert
        assertThat(reserva).containsExactly(
                "2026-07-01 | 100.00 | 0.00 | 0.00 | 100.00",
                "2026-08-01 | 0.00 | -350.00 | 100.00 | -250.00");
    }

    @Test
    void reservaIncluiOMesEmAndamentoEDeixaDeForaOsMesesFuturos() {
        // Arrange
        YearMonth mesAtual = YearMonth.now(SAO_PAULO);
        inserirAjuste(mesAtual.atDay(1).toString(), "100.00", "saldo antes do sistema");
        new Lancamento("RECEITA", "50.00", trabalhoA, marco, mesAtual.atDay(1).toString()).inserir();
        new Lancamento("DESPESA", "20.00", alimentacao, casa, mesAtual.plusMonths(1).atDay(1).toString())
                .dataCompra(mesAtual.atDay(1).toString()).inserir();
        new Lancamento("DESPESA", "30.00", lazer, ana, "2099-01-01").inserir();
        inserirAjuste("2099-06-01", "999.00", "ajuste com data futura");

        // Act
        List<String> reserva = reservaAte("2099-12-01");

        // Assert: só o mês em andamento aparece; a parcela do mês seguinte e o que está em 2099
        // só vão contar quando o mês deles chegar (RF18).
        assertThat(reserva).containsExactly(
                mesAtual.atDay(1) + " | 100.00 | 50.00 | 0.00 | 150.00");
    }

    // ----- apoio -----

    private List<String> reservaAte(String ultimoMes) {
        return jdbc.sql("""
                        SELECT mes_referencia, ajustes_mes, resultado_mes, reserva_inicio_mes,
                               reserva_fim_mes
                          FROM visao_reserva_mensal
                         WHERE mes_referencia <= ?
                         ORDER BY mes_referencia
                        """)
                .param(ultimoMes)
                .query((rs, n) -> String.join(" | ",
                        rs.getString("mes_referencia"), rs.getString("ajustes_mes"),
                        rs.getString("resultado_mes"), rs.getString("reserva_inicio_mes"),
                        rs.getString("reserva_fim_mes")))
                .list();
    }

    /** Um lançamento válido; cada teste muda só o que quer provar. */
    private final class Lancamento {
        private final String tipo;
        private final String valor;
        private final long categoriaId;
        private final long destinatarioId;
        private final String mesReferencia;
        private String descricao = "Lançamento de teste";
        private String dataCompra;
        private Long cartaoId;
        private boolean excluido;

        Lancamento(String tipo, String valor, long categoriaId, long destinatarioId, String mesReferencia) {
            this.tipo = tipo;
            this.valor = valor;
            this.categoriaId = categoriaId;
            this.destinatarioId = destinatarioId;
            this.mesReferencia = mesReferencia;
            this.dataCompra = mesReferencia;
        }

        Lancamento descricao(String nova) {
            descricao = nova;
            return this;
        }

        Lancamento dataCompra(String nova) {
            dataCompra = nova;
            return this;
        }

        Lancamento cartao(long novo) {
            cartaoId = novo;
            return this;
        }

        /** Exclusão lógica (ADR-006), como a T16 fará com as parcelas abatidas. */
        Lancamento excluido() {
            excluido = true;
            return this;
        }

        void inserir() {
            jdbc.sql("""
                            INSERT INTO lancamento (tipo, valor, descricao, categoria_id, destinatario_id,
                                                    data_compra, mes_referencia, origem, cartao_id,
                                                    excluido_em, %s)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'WEB', ?, ?, %s)
                            """.formatted(COLUNAS_C7, VALORES_C7))
                    .params(tipo, valor, descricao, categoriaId, destinatarioId, dataCompra,
                            mesReferencia, cartaoId, excluido ? "2026-11-01 12:00:00.000" : null)
                    .update();
        }
    }

    private long inserirCategoria(String nome, String tipo) {
        jdbc.sql("INSERT INTO categoria (nome, tipo, %s) VALUES (?, ?, %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(nome, tipo)
                .update();
        return ultimoIdGerado();
    }

    private long inserirDestinatario(String nome, String marcacao) {
        jdbc.sql("INSERT INTO destinatario (nome, marcacao_bot, %s) VALUES (?, ?, %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(nome, marcacao)
                .update();
        return ultimoIdGerado();
    }

    private void inserirOrcamento(long categoria, String vigenteDesde, String valorPlanejado) {
        jdbc.sql("""
                        INSERT INTO orcamento (categoria_id, vigente_desde, valor_planejado, %s)
                        VALUES (?, ?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(categoria, vigenteDesde, valorPlanejado)
                .update();
    }

    private void inserirMeta(String vigenteDesde, String percentual) {
        jdbc.sql("INSERT INTO meta_economia (vigente_desde, percentual, %s) VALUES (?, ?, %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(vigenteDesde, percentual)
                .update();
    }

    private void inserirAjuste(String data, String valor, String motivo) {
        jdbc.sql("INSERT INTO ajuste_reserva (data, valor, motivo, %s) VALUES (?, ?, ?, %s)"
                        .formatted(COLUNAS_C7, VALORES_C7))
                .params(data, valor, motivo)
                .update();
    }

    private long ultimoIdGerado() {
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    private static final String COLUNAS_C7 =
            "criado_em, criado_por, atualizado_em, atualizado_por, versao";

    private static final String VALORES_C7 =
            "UTC_TIMESTAMP(3), 'teste', UTC_TIMESTAMP(3), 'teste', 0";
}
