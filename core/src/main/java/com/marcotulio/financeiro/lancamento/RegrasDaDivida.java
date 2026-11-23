package com.marcotulio.financeiro.lancamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * As regras de uma dívida que não dependem do banco (RF12, ADR-005).
 *
 * <p>Como as {@link RegrasDoLancamento}, são funções puras, testadas sem Spring e sem MySQL. Cada
 * parcela, depois, passa também pelas regras de um lançamento comum.
 */
final class RegrasDaDivida {

    private static final int MINIMO_DE_PARCELAS = 2;

    private RegrasDaDivida() {
    }

    /** Confere o que não pode faltar, antes de o serviço buscar a categoria e as faturas. */
    static void exigirCamposObrigatorios(DadosDaDivida dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Informe os dados da dívida.");
        }
        exigir(dados.tipo() != null, "Informe o tipo da dívida.");
        exigir(dados.descricao() != null && !dados.descricao().isBlank(), "Informe a descrição.");
        exigir(dados.quantidadeParcelas() != null, "Informe a quantidade de parcelas.");
        exigir(dados.quantidadeParcelas() >= MINIMO_DE_PARCELAS,
                "Uma dívida tem pelo menos " + MINIMO_DE_PARCELAS + " parcelas.");
        exigir(dados.valorParcela() != null || dados.valorTotal() != null,
                "Informe o valor da parcela ou o valor total.");
        exigir(dados.valorParcela() == null || dados.valorTotal() == null,
                "Informe só o valor da parcela ou só o valor total, não os dois.");
        exigir(dados.destinatarioId() != null, "Informe para quem é a dívida.");
        exigir(dados.dataContratacao() != null, "Informe a data da compra ou da contratação.");
        exigir(dados.tipo() != TipoDivida.PARCELAMENTO_CARTAO || dados.cartaoId() != null,
                "Informe o cartão do parcelamento.");
    }

    /**
     * O valor de cada parcela, da primeira à última (ADR-005). O total da dívida é a soma da lista.
     *
     * <ul>
     *   <li>Pelo valor da parcela: todas iguais, e o total é a multiplicação exata.</li>
     *   <li>Pelo total: a divisão arredonda com {@code HALF_EVEN} (o arredondamento bancário) e a
     *       diferença vai para a primeira parcela. Assim a soma é exatamente o total.</li>
     * </ul>
     *
     * @param dados já aprovados por {@link #exigirCamposObrigatorios}
     */
    static List<BigDecimal> valoresDasParcelas(DadosDaDivida dados) {
        int quantidade = dados.quantidadeParcelas();
        if (dados.valorParcela() != null) {
            BigDecimal parcela = RegrasDoLancamento.valorEmCentavos(dados.valorParcela());
            return Collections.nCopies(quantidade, parcela);
        }
        BigDecimal total = RegrasDoLancamento.valorEmCentavos(dados.valorTotal());
        BigDecimal demais = total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_EVEN);
        BigDecimal primeira = total.subtract(demais.multiply(BigDecimal.valueOf(quantidade - 1L)));
        exigir(demais.signum() > 0 && primeira.signum() > 0,
                "O valor total é pequeno demais para " + quantidade + " parcelas.");

        List<BigDecimal> parcelas = new ArrayList<>(Collections.nCopies(quantidade, demais));
        parcelas.set(0, primeira);
        return parcelas;
    }

    /** A descrição do item com o número da parcela, preservada em cada uma (RF12): "celular 3/16". */
    static String descricaoDaParcela(String descricao, int numero, int quantidade) {
        return descricao.strip() + " " + numero + "/" + quantidade;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
