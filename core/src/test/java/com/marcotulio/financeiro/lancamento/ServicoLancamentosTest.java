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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Registro e edição de lançamentos gravando de verdade no MySQL (RF01, RF13, RN02).
 *
 * <p>Como o {@code ServicoFaturasTest}, esta classe não é {@code @Transactional}: a fatura de uma
 * compra no cartão é criada numa transação própria, que precisa enxergar o cartão já confirmado
 * no banco. Por isso cada teste grava de fato, e o {@code @AfterEach} apaga o que foi gravado
 * (só no teste: a aplicação nunca exclui).
 */
class ServicoLancamentosTest extends TesteDeIntegracao {

    @Autowired
    private ServicoLancamentos servico;

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

    private long alimentacao;
    private long salario;
    private long casa;

    /** Banco A do Marco: fecha no dia 20 e vence no dia 25 (ERS, Apêndice B). */
    private long amt;

    @BeforeEach
    void cadastraOQueUmLancamentoUsa() {
        alimentacao = categorias.cadastrarSeAusente(
                "Alimentação de teste", TipoCategoria.DESPESA, true).id();
        salario = categorias.cadastrarSeAusente("Salário de teste", TipoCategoria.RECEITA, true).id();
        casa = destinatarios.cadastrarSeAusente("Casa", "@casa").id();
        destinatarios.cadastrarSeAusente("Marco", "@mt");
        usuarios.cadastrarSeAusente("Marco", "marco", "senha-de-teste", "@mt");
        amt = cartoes.cadastrarSeAusente(
                "amt", "Banco A", "marco", new RegraCartao(20, 25, CompraNoFechamento.PROXIMA)).id();
    }

    @AfterEach
    void apagaOQueOTesteGravou() {
        jdbc.sql("DELETE FROM lancamento").update();
        jdbc.sql("DELETE FROM fatura").update();
        jdbc.sql("DELETE FROM cartao").update();
        jdbc.sql("DELETE FROM usuario WHERE login = 'marco'").update();
        jdbc.sql("DELETE FROM destinatario WHERE marcacao_bot IN ('@mt', '@casa')").update();
        jdbc.sql("DELETE FROM categoria WHERE nome IN ('Alimentação de teste', 'Salário de teste')").update();
    }

    private ExemploDeLancamento exemplo() {
        return new ExemploDeLancamento(alimentacao, casa);
    }

    // ----- registro -----

    @Test
    void registraDespesaSemCartaoNoMesDaCompra() {
        // Act: R$ 150,00 de mercado para a casa, em 10/10/2026.
        LancamentoRegistrado registrado = servico.registrar(exemplo().dados(), OrigemLancamento.WEB);

        // Assert
        assertThat(registrado.id()).isPositive();
        assertThat(registrado.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(registrado.cartaoId()).isNull();
        assertThat(registrado.faturaId()).isNull();
        Map<String, Object> linha = linhaDoLancamento(registrado.id());
        assertThat(linha.get("tipo")).isEqualTo("DESPESA");
        assertThat(linha.get("valor")).hasToString("150.00");
        assertThat(linha.get("descricao")).isEqualTo("Mercado");
        assertThat(linha.get("categoria_id")).isEqualTo(alimentacao);
        assertThat(linha.get("destinatario_id")).isEqualTo(casa);
        assertThat(linha.get("data_compra")).hasToString("2026-10-10");
        assertThat(linha.get("mes_referencia")).hasToString("2026-10-01");
        assertThat(linha.get("origem")).isEqualTo("WEB");
        assertThat(linha.get("revisar")).isEqualTo(false);
    }

    @Test
    void registraOAutorDaGravacao() {
        // Act: sem ninguém logado (como na importação), o autor é o sistema (C7).
        LancamentoRegistrado registrado = servico.registrar(exemplo().dados(), OrigemLancamento.WEB);

        // Assert
        assertThat(linhaDoLancamento(registrado.id()).get("criado_por")).isEqualTo("sistema");
    }

    @Test
    void registraReceitaImportadaDaPlanilha() {
        // Arrange
        DadosDoLancamento receita = exemplo()
                .tipo(TipoLancamento.RECEITA)
                .categoria(salario)
                .descricao("Trabalho A")
                .valor("5000")
                .dados();

        // Act
        LancamentoRegistrado registrado = servico.registrar(receita, OrigemLancamento.IMPORTACAO);

        // Assert
        assertThat(registrado.origem()).isEqualTo(OrigemLancamento.IMPORTACAO);
        assertThat(registrado.valor()).hasToString("5000.00");
        assertThat(linhaDoLancamento(registrado.id()).get("origem")).isEqualTo("IMPORTACAO");
    }

    @Test
    void compraNoCartaoEntraNaFaturaENoMesDeReferenciaDela() {
        // Arrange: 21/10/2026 é depois do fechamento do dia 20, então a fatura é a de novembro.
        DadosDoLancamento noCartao = exemplo().cartao(amt).dataCompra(LocalDate.of(2026, 10, 21)).dados();

        // Act
        LancamentoRegistrado registrado = servico.registrar(noCartao, OrigemLancamento.WEB);

        // Assert: o mês do lançamento e o da fatura são o mesmo (modelo de dados, seção 3.7).
        assertThat(registrado.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(registrado.cartaoId()).isEqualTo(amt);
        assertThat(registrado.faturaId()).isNotNull();
        String mesDaFatura = jdbc.sql("SELECT mes_referencia FROM fatura WHERE id = ?")
                .param(registrado.faturaId())
                .query(String.class)
                .single();
        assertThat(mesDaFatura).isEqualTo("2026-11-01");
        assertThat(linhaDoLancamento(registrado.id()).get("mes_referencia")).hasToString(mesDaFatura);
    }

    @Test
    void mesInformadoPrevaleceSobreAFaturaEACompraFicaForaDela() {
        // Arrange: pela fatura seria novembro; quem lançou informou dezembro.
        DadosDoLancamento noCartao = exemplo()
                .cartao(amt)
                .dataCompra(LocalDate.of(2026, 10, 21))
                .mesReferencia(YearMonth.of(2026, 12))
                .dados();

        // Act
        LancamentoRegistrado registrado = servico.registrar(noCartao, OrigemLancamento.WEB);

        // Assert
        assertThat(registrado.mesReferencia()).isEqualTo(YearMonth.of(2026, 12));
        assertThat(registrado.cartaoId()).isEqualTo(amt);
        assertThat(registrado.faturaId()).isNull();
    }

    @Test
    void mesInformadoPrevaleceSobreOMesDaCompra() {
        // Act
        LancamentoRegistrado registrado = servico.registrar(
                exemplo().mesReferencia(YearMonth.of(2026, 11)).dados(), OrigemLancamento.WEB);

        // Assert
        assertThat(linhaDoLancamento(registrado.id()).get("mes_referencia")).hasToString("2026-11-01");
    }

    @Test
    void recusaMesDeReferenciaAnteriorAoDaCompra() {
        // Arrange
        DadosDoLancamento anterior = exemplo().mesReferencia(YearMonth.of(2026, 9)).dados();

        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(anterior, OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser anterior");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaLancamentoSemParaQuem() {
        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(exemplo().paraQuem(null).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe para quem é o lançamento.");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaParaQuemInexistente() {
        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(exemplo().paraQuem(-1L).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Para quem não encontrado: -1.");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaCategoriaInexistente() {
        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(exemplo().categoria(-1L).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoria não encontrada: -1.");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaCategoriaDeOutroTipo() {
        // Act e Assert: uma despesa classificada como salário.
        assertThatThrownBy(() -> servico.registrar(exemplo().categoria(salario).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("é de receita e não pode classificar uma despesa");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaCartaoInexistente() {
        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(exemplo().cartao(-1L).dados(), OrigemLancamento.WEB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cartão não encontrado");
        assertThat(contarLancamentos()).isZero();
    }

    @Test
    void recusaLancamentoSemOrigem() {
        // Act e Assert
        assertThatThrownBy(() -> servico.registrar(exemplo().dados(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a origem do lançamento.");
        assertThat(contarLancamentos()).isZero();
    }

    // ----- edição -----

    @Test
    void edicaoAlteraOsDadosERecalculaOMesDeReferencia() {
        // Arrange
        LancamentoRegistrado original = servico.registrar(exemplo().dados(), OrigemLancamento.IMPORTACAO);
        DadosDoLancamento corrigido = exemplo()
                .valor("99.90")
                .descricao("Feira")
                .cartao(amt)
                .dataCompra(LocalDate.of(2026, 10, 21))
                .dados();

        // Act
        LancamentoRegistrado editado = servico.editar(original.id(), corrigido);

        // Assert: a origem é de quando o lançamento nasceu e não muda na edição.
        assertThat(editado.id()).isEqualTo(original.id());
        assertThat(editado.origem()).isEqualTo(OrigemLancamento.IMPORTACAO);
        Map<String, Object> linha = linhaDoLancamento(original.id());
        assertThat(linha.get("valor")).hasToString("99.90");
        assertThat(linha.get("descricao")).isEqualTo("Feira");
        assertThat(linha.get("cartao_id")).isEqualTo(amt);
        assertThat(linha.get("fatura_id")).isEqualTo(editado.faturaId());
        assertThat(linha.get("mes_referencia")).hasToString("2026-11-01");
        assertThat(linha.get("versao")).isEqualTo(1L);
    }

    @Test
    void edicaoQueTiraOCartaoTiraTambemAFatura() {
        // Arrange
        LancamentoRegistrado noCartao = servico.registrar(
                exemplo().cartao(amt).dataCompra(LocalDate.of(2026, 10, 21)).dados(), OrigemLancamento.WEB);

        // Act
        LancamentoRegistrado editado = servico.editar(noCartao.id(), exemplo().dados());

        // Assert
        assertThat(editado.cartaoId()).isNull();
        assertThat(editado.faturaId()).isNull();
        assertThat(editado.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(linhaDoLancamento(noCartao.id()).get("fatura_id")).isNull();
    }

    @Test
    void edicaoInvalidaNaoAlteraNada() {
        // Arrange
        LancamentoRegistrado original = servico.registrar(exemplo().dados(), OrigemLancamento.WEB);

        // Act e Assert
        assertThatThrownBy(() -> servico.editar(original.id(), exemplo().valor("0").dados()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(linhaDoLancamento(original.id()).get("valor")).hasToString("150.00");
    }

    @Test
    void edicaoDeLancamentoInexistenteFalha() {
        // Act e Assert
        assertThatThrownBy(() -> servico.editar(-1L, exemplo().dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lançamento não encontrado: -1.");
    }

    // ----- sem exclusão no MVP 1 -----

    @Test
    void naoHaComoExcluirUmLancamento() {
        // Arrange: a exclusão é lógica e exige validação cruzada (RF10), fora do MVP 1.
        // Act
        var metodosDoServico = Arrays.stream(ServicoLancamentos.class.getMethods())
                .map(metodo -> metodo.getName().toLowerCase())
                .filter(nome -> nome.contains("exclu") || nome.contains("delet") || nome.contains("remov"))
                .toList();
        var metodosDoRepositorio = Arrays.stream(RepositorioDeLancamentos.class.getMethods())
                .filter(metodo -> metodo.getName().startsWith("delete"))
                .toList();

        // Assert
        assertThat(metodosDoServico).isEmpty();
        assertThat(metodosDoRepositorio).isEmpty();
    }

    // ----- auxiliares -----

    private Map<String, Object> linhaDoLancamento(long id) {
        return jdbc.sql("SELECT * FROM lancamento WHERE id = ?").param(id).query().singleRow();
    }

    private int contarLancamentos() {
        return jdbc.sql("SELECT COUNT(*) FROM lancamento").query(Integer.class).single();
    }
}
