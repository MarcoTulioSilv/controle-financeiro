package com.marcotulio.financeiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.cadastro.CategoriaCadastrada;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import com.marcotulio.financeiro.cartao.FaturaRegistrada;
import com.marcotulio.financeiro.cartao.StatusFatura;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/**
 * Regras de um lançamento que não dependem do banco (RF01, RF13, RN02, ADR-005). Testes unitários
 * puros: sem Spring e sem MySQL.
 */
class RegrasDoLancamentoTest {

    private static final long CATEGORIA = 10L;
    private static final long CASA = 20L;
    private static final long CARTAO = 30L;

    private static final CategoriaCadastrada ALIMENTACAO =
            new CategoriaCadastrada(CATEGORIA, "Alimentação", TipoCategoria.DESPESA, true);

    private static final CategoriaCadastrada SALARIO =
            new CategoriaCadastrada(CATEGORIA, "Salário", TipoCategoria.RECEITA, true);

    private static ExemploDeLancamento exemplo() {
        return new ExemploDeLancamento(CATEGORIA, CASA);
    }

    /** Uma fatura aberta do cartão de teste no mês indicado. */
    private static FaturaRegistrada faturaDe(YearMonth mesReferencia) {
        LocalDate vencimento = mesReferencia.atDay(5);
        return new FaturaRegistrada(99L, CARTAO, mesReferencia, vencimento.minusDays(10), vencimento,
                vencimento, StatusFatura.ABERTA);
    }

    // ----- campos obrigatórios -----

    @Test
    void dadosCompletosPassamNaConferenciaDosObrigatorios() {
        // Act e Assert: não lança exceção.
        RegrasDoLancamento.exigirCamposObrigatorios(exemplo().dados());
    }

    @Test
    void paraQuemEhObrigatorio() {
        // Arrange: o sistema nunca supõe a quem o lançamento se destina (RF13).
        DadosDoLancamento semParaQuem = exemplo().paraQuem(null).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(semParaQuem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe para quem é o lançamento.");
    }

    @Test
    void tipoEhObrigatorio() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().tipo(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe se é receita ou despesa.");
    }

    @Test
    void valorEhObrigatorio() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().valor(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o valor.");
    }

    @Test
    void categoriaEhObrigatoria() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().categoria(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a categoria.");
    }

    @Test
    void dataDaCompraEhObrigatoria() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().dataCompra(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a data da compra.");
    }

    @Test
    void dadosAusentesSaoRecusados() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----- valor (ADR-005) -----

    @Test
    void valorGuardaSempreDuasCasasDecimais() {
        // Act
        LancamentoConferido inteiro = conferir(exemplo().valor("150"));
        LancamentoConferido umaCasa = conferir(exemplo().valor("150.5"));
        LancamentoConferido zerosAMais = conferir(exemplo().valor("150.500"));

        // Assert: hasToString confere também a escala, que o equals do BigDecimal considera.
        assertThat(inteiro.valor()).hasToString("150.00");
        assertThat(umaCasa.valor()).hasToString("150.50");
        assertThat(zerosAMais.valor()).hasToString("150.50");
    }

    @Test
    void umCentavoEhOMenorValorAceito() {
        // Act
        LancamentoConferido conferido = conferir(exemplo().valor("0.01"));

        // Assert
        assertThat(conferido.valor()).hasToString("0.01");
    }

    @Test
    void valorZeroEhRecusado() {
        assertThatThrownBy(() -> conferir(exemplo().valor("0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor deve ser maior que zero.");
    }

    @Test
    void valorNegativoEhRecusado() {
        // O sentido do dinheiro vem do tipo (receita ou despesa), nunca do sinal (C4).
        assertThatThrownBy(() -> conferir(exemplo().valor("-150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor deve ser maior que zero.");
    }

    @Test
    void fracaoDeCentavoEhRecusadaEmVezDeArredondada() {
        // Arredondar em silêncio mudaria o valor que o usuário digitou.
        assertThatThrownBy(() -> conferir(exemplo().valor("150.555")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor aceita no máximo dois decimais (centavos).");
    }

    // ----- descrição -----

    @Test
    void descricaoPerdeOsEspacosDasPontas() {
        // Act
        LancamentoConferido conferido = conferir(exemplo().descricao("  Mercado  "));

        // Assert
        assertThat(conferido.descricao()).isEqualTo("Mercado");
    }

    @Test
    void descricaoEmBrancoEhRecusada() {
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().descricao("   ").dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a descrição.");
        assertThatThrownBy(() -> RegrasDoLancamento.exigirCamposObrigatorios(exemplo().descricao(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a descrição.");
    }

    @Test
    void descricaoAceitaAte200Caracteres() {
        // Arrange
        String limite = "a".repeat(200);

        // Act
        LancamentoConferido conferido = conferir(exemplo().descricao(limite));

        // Assert
        assertThat(conferido.descricao()).hasSize(200);
    }

    @Test
    void descricaoCom201CaracteresEhRecusada() {
        assertThatThrownBy(() -> conferir(exemplo().descricao("a".repeat(201))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A descrição aceita no máximo 200 caracteres.");
    }

    @Test
    void emojiContaComoUmCaractereNaDescricao() {
        // Arrange: em Java, um emoji ocupa duas posições da String; no MySQL (utf8mb4), um caractere.
        String limite = "a".repeat(199) + "🛒";

        // Act
        LancamentoConferido conferido = conferir(exemplo().descricao(limite));

        // Assert
        assertThat(conferido.descricao()).isEqualTo(limite);
    }

    // ----- categoria coerente com o tipo -----

    @Test
    void despesaNaoAceitaCategoriaDeReceita() {
        assertThatThrownBy(() -> RegrasDoLancamento.conferir(exemplo().dados(), SALARIO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A categoria Salário é de receita e não pode classificar uma despesa.");
    }

    @Test
    void receitaNaoAceitaCategoriaDeDespesa() {
        DadosDoLancamento receita = exemplo().tipo(TipoLancamento.RECEITA).dados();

        assertThatThrownBy(() -> RegrasDoLancamento.conferir(receita, ALIMENTACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A categoria Alimentação é de despesa e não pode classificar uma receita.");
    }

    @Test
    void receitaAceitaCategoriaDeReceita() {
        // Act
        LancamentoConferido conferido = RegrasDoLancamento.conferir(
                exemplo().tipo(TipoLancamento.RECEITA).dados(), SALARIO, null);

        // Assert
        assertThat(conferido.tipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(conferido.categoriaId()).isEqualTo(CATEGORIA);
    }

    // ----- mês de referência (RF01, RN02) -----

    @Test
    void semCartaoOMesDeReferenciaEhOMesDaCompra() {
        // Act
        LancamentoConferido conferido = conferir(exemplo().dataCompra(LocalDate.of(2026, 10, 31)));

        // Assert
        assertThat(conferido.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(conferido.cartaoId()).isNull();
        assertThat(conferido.faturaId()).isNull();
    }

    @Test
    void comCartaoOMesDeReferenciaEhODaFatura() {
        // Arrange
        DadosDoLancamento noCartao = exemplo().cartao(CARTAO).dados();

        // Act
        LancamentoConferido conferido =
                RegrasDoLancamento.conferir(noCartao, ALIMENTACAO, faturaDe(YearMonth.of(2026, 11)));

        // Assert
        assertThat(conferido.mesReferencia()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(conferido.cartaoId()).isEqualTo(CARTAO);
        assertThat(conferido.faturaId()).isEqualTo(99L);
    }

    @Test
    void mesInformadoPrevaleceSobreOMesDaCompra() {
        // Act
        LancamentoConferido conferido = conferir(exemplo().mesReferencia(YearMonth.of(2026, 12)));

        // Assert
        assertThat(conferido.mesReferencia()).isEqualTo(YearMonth.of(2026, 12));
    }

    @Test
    void mesInformadoIgualAoDaCompraEhAceito() {
        // Act: o limite da RN02 é o próprio mês da compra.
        LancamentoConferido conferido = conferir(exemplo().mesReferencia(YearMonth.of(2026, 10)));

        // Assert
        assertThat(conferido.mesReferencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void mesInformadoIgualAoDaFaturaMantemOLancamentoNaFatura() {
        // Arrange
        DadosDoLancamento noCartao =
                exemplo().cartao(CARTAO).mesReferencia(YearMonth.of(2026, 11)).dados();

        // Act
        LancamentoConferido conferido =
                RegrasDoLancamento.conferir(noCartao, ALIMENTACAO, faturaDe(YearMonth.of(2026, 11)));

        // Assert
        assertThat(conferido.faturaId()).isEqualTo(99L);
    }

    @Test
    void mesInformadoDiferenteDoDaFaturaPrevaleceEDesligaOLancamentoDaFatura() {
        // Arrange
        DadosDoLancamento noCartao =
                exemplo().cartao(CARTAO).mesReferencia(YearMonth.of(2026, 12)).dados();

        // Act
        LancamentoConferido conferido =
                RegrasDoLancamento.conferir(noCartao, ALIMENTACAO, faturaDe(YearMonth.of(2026, 11)));

        // Assert: o mês da fatura e o do lançamento nunca divergem (modelo de dados, seção 3.7).
        assertThat(conferido.mesReferencia()).isEqualTo(YearMonth.of(2026, 12));
        assertThat(conferido.cartaoId()).isEqualTo(CARTAO);
        assertThat(conferido.faturaId()).isNull();
    }

    @Test
    void mesInformadoAnteriorAoDaCompraEhRecusado() {
        // Arrange: compra em 01/10/2026, um dia depois do fim de setembro.
        DadosDoLancamento anterior = exemplo()
                .dataCompra(LocalDate.of(2026, 10, 1))
                .mesReferencia(YearMonth.of(2026, 9))
                .dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDoLancamento.conferir(anterior, ALIMENTACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O mês de referência (09/2026) não pode ser anterior ao mês da compra (10/2026).");
    }

    @Test
    void mesInformadoDoAnoAnteriorEhRecusado() {
        // Arrange: dezembro vem antes de janeiro só no ano anterior.
        DadosDoLancamento anterior = exemplo()
                .dataCompra(LocalDate.of(2027, 1, 15))
                .mesReferencia(YearMonth.of(2026, 12))
                .dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDoLancamento.conferir(anterior, ALIMENTACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser anterior");
    }

    private static LancamentoConferido conferir(ExemploDeLancamento exemplo) {
        return RegrasDoLancamento.conferir(exemplo.dados(), ALIMENTACAO, null);
    }
}
