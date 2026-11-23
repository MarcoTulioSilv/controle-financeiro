package com.marcotulio.financeiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regras de uma dívida que não dependem do banco (RF12, ADR-005). Testes unitários puros: sem
 * Spring e sem MySQL.
 */
class RegrasDaDividaTest {

    private static final long CASA = 20L;
    private static final long BMT = 30L;

    private static ExemploDeDivida exemplo() {
        return new ExemploDeDivida(CASA, BMT);
    }

    // ----- campos obrigatórios -----

    @Test
    void dadosCompletosPassamNaConferenciaDosObrigatorios() {
        // Act e Assert: não lança exceção.
        RegrasDaDivida.exigirCamposObrigatorios(exemplo().dados());
        RegrasDaDivida.exigirCamposObrigatorios(exemplo().totalDe("1000.00").dados());
    }

    @Test
    void dadosAusentesSaoRecusados() {
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe os dados da dívida.");
    }

    @Test
    void paraQuemEhObrigatorio() {
        // Arrange: como num lançamento, o sistema nunca supõe a quem a dívida se destina (RF13).
        DadosDaDivida semParaQuem = exemplo().paraQuem(null).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(semParaQuem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe para quem é a dívida.");
    }

    @Test
    void tipoEhObrigatorio() {
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(exemplo().tipo(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o tipo da dívida.");
    }

    @Test
    void descricaoEhObrigatoria() {
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(exemplo().descricao(" ").dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a descrição.");
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(exemplo().descricao(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a descrição.");
    }

    @Test
    void quantidadeDeParcelasEhObrigatoria() {
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(exemplo().quantidade(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a quantidade de parcelas.");
    }

    @Test
    void dataDaCompraEhObrigatoria() {
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(exemplo().dataContratacao(null).dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe a data da compra ou da contratação.");
    }

    @Test
    void umaDividaTemPeloMenosDuasParcelas() {
        // Arrange: uma parcela só é uma compra à vista, na categoria Compras (RF12).
        DadosDaDivida umaParcela = exemplo().quantidade(1).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(umaParcela))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uma dívida tem pelo menos 2 parcelas.");
    }

    @Test
    void exigeOValorDaParcelaOuOTotal() {
        // Arrange
        DadosDaDivida semValor = exemplo().parcelasDe(null).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(semValor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o valor da parcela ou o valor total.");
    }

    @Test
    void naoAceitaOValorDaParcelaEOTotalAoMesmoTempo() {
        // Arrange: os dois juntos poderiam se contradizer (16 × 106,18 ≠ 1.700,00).
        DadosDaDivida osDois = exemplo().parcelaETotal("106.18", "1700.00").dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(osDois))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe só o valor da parcela ou só o valor total, não os dois.");
    }

    @Test
    void parcelamentoDeCartaoExigeOCartao() {
        // Arrange
        DadosDaDivida semCartao = exemplo().cartao(null).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.exigirCamposObrigatorios(semCartao))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe o cartão do parcelamento.");
    }

    @Test
    void financiamentoEEmprestimoNaoExigemCartao() {
        // Act e Assert: não lança exceção.
        RegrasDaDivida.exigirCamposObrigatorios(exemplo().tipo(TipoDivida.FINANCIAMENTO).cartao(null).dados());
        RegrasDaDivida.exigirCamposObrigatorios(exemplo().tipo(TipoDivida.EMPRESTIMO).cartao(null).dados());
    }

    // ----- valores das parcelas (ADR-005) -----

    @Test
    void informadoPeloValorDaParcelaOTotalEhAMultiplicacaoExata() {
        // Act: celular 16x 106,18 (ERS, seção 4.1).
        List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(exemplo().parcelasDe("106.18").dados());

        // Assert
        assertThat(parcelas).hasSize(16).allSatisfy(parcela -> assertThat(parcela).isEqualTo("106.18"));
        assertThat(soma(parcelas)).isEqualTo("1698.88");
    }

    @Test
    void valorDaParcelaGanhaEscala2() {
        // Act
        List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(
                exemplo().parcelasDe("50").quantidade(2).dados());

        // Assert: isEqualTo compara também a escala, como o BigDecimal.equals.
        assertThat(parcelas).containsExactly(new BigDecimal("50.00"), new BigDecimal("50.00"));
    }

    @Test
    void informadoPeloTotalDivisivelTodasAsParcelasSaoIguais() {
        // Act
        List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(
                exemplo().totalDe("1200.00").quantidade(12).dados());

        // Assert
        assertThat(parcelas).hasSize(12).allSatisfy(parcela -> assertThat(parcela).isEqualTo("100.00"));
    }

    @Test
    void informadoPeloTotalADiferencaDoArredondamentoVaiParaAPrimeiraParcela() {
        // Act: 100,00 / 3 = 33,333... → 33,33; a primeira fica com o centavo que sobra.
        List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(
                exemplo().totalDe("100.00").quantidade(3).dados());

        // Assert
        assertThat(parcelas).containsExactly(
                new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(soma(parcelas)).isEqualTo("100.00");
    }

    @Test
    void quandoOArredondamentoSobeAPrimeiraParcelaFicaMenor() {
        // Act: 200,00 / 3 = 66,666... → 66,67; a diferença (−0,01) também vai para a primeira.
        List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(
                exemplo().totalDe("200.00").quantidade(3).dados());

        // Assert
        assertThat(parcelas).containsExactly(
                new BigDecimal("66.66"), new BigDecimal("66.67"), new BigDecimal("66.67"));
        assertThat(soma(parcelas)).isEqualTo("200.00");
    }

    @Test
    void oEmpateNoMeioCentavoArredondaParaOParComoNoArredondamentoBancario() {
        // Act: 0,25 / 2 = 0,125 e 0,35 / 2 = 0,175. O HALF_EVEN leva o empate ao centavo par
        // (0,12 e 0,18); o HALF_UP, da escola, levaria sempre para cima (0,13 e 0,18).
        List<BigDecimal> de25Centavos = RegrasDaDivida.valoresDasParcelas(
                exemplo().totalDe("0.25").quantidade(2).dados());
        List<BigDecimal> de35Centavos = RegrasDaDivida.valoresDasParcelas(
                exemplo().totalDe("0.35").quantidade(2).dados());

        // Assert
        assertThat(de25Centavos).containsExactly(new BigDecimal("0.13"), new BigDecimal("0.12"));
        assertThat(de35Centavos).containsExactly(new BigDecimal("0.17"), new BigDecimal("0.18"));
    }

    /**
     * O critério do ADR-005: a soma das parcelas é exatamente o total, sem centavo a mais ou a
     * menos, para qualquer combinação de total e quantidade.
     */
    @Test
    void aSomaDasParcelasEhExatamenteOTotal() {
        // Arrange
        List<String> totais = List.of("1698.88", "100.00", "200.00", "1000.01", "9999.99", "12345.67", "29.97");

        for (String total : totais) {
            for (int quantidade = 2; quantidade <= 48; quantidade++) {
                // Act
                List<BigDecimal> parcelas = RegrasDaDivida.valoresDasParcelas(
                        exemplo().totalDe(total).quantidade(quantidade).dados());

                // Assert
                String caso = total + " em " + quantidade + "x";
                assertThat(soma(parcelas)).as(caso).isEqualTo(total);
                assertThat(parcelas).as(caso).hasSize(quantidade)
                        .allSatisfy(parcela -> {
                            assertThat(parcela.scale()).isEqualTo(2);
                            assertThat(parcela.signum()).isPositive();
                        });
            }
        }
    }

    @Test
    void totalPequenoDemaisParaAsParcelasEhRecusado() {
        // Arrange: 0,01 em 2x deixaria uma parcela de R$ 0,00; 0,30 em 20x daria parcelas de
        // 0,02 (0,015 arredondado) e uma primeira negativa (0,30 − 19 × 0,02 = −0,08).
        DadosDaDivida umCentavo = exemplo().totalDe("0.01").quantidade(2).dados();
        DadosDaDivida trintaCentavos = exemplo().totalDe("0.30").quantidade(20).dados();

        // Act e Assert
        assertThatThrownBy(() -> RegrasDaDivida.valoresDasParcelas(umCentavo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor total é pequeno demais para 2 parcelas.");
        assertThatThrownBy(() -> RegrasDaDivida.valoresDasParcelas(trintaCentavos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor total é pequeno demais para 20 parcelas.");
    }

    @Test
    void valorDaParcelaOuTotalPrecisaSerPositivoEEmCentavos() {
        // Act e Assert: as mesmas regras de valor de um lançamento (ADR-005).
        assertThatThrownBy(() -> RegrasDaDivida.valoresDasParcelas(exemplo().parcelasDe("0").dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor deve ser maior que zero.");
        assertThatThrownBy(() -> RegrasDaDivida.valoresDasParcelas(exemplo().totalDe("-10.00").dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor deve ser maior que zero.");
        assertThatThrownBy(() -> RegrasDaDivida.valoresDasParcelas(exemplo().parcelasDe("106.185").dados()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O valor aceita no máximo dois decimais (centavos).");
    }

    // ----- descrição de cada parcela -----

    @Test
    void cadaParcelaPreservaADescricaoComONumeroEAQuantidade() {
        // Act e Assert: "celular 3/16" (RF12).
        assertThat(RegrasDaDivida.descricaoDaParcela("celular", 3, 16)).isEqualTo("celular 3/16");
    }

    private static BigDecimal soma(List<BigDecimal> parcelas) {
        return parcelas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
