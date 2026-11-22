package com.marcotulio.financeiro.lancamento;

import com.marcotulio.financeiro.cadastro.CategoriaCadastrada;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import com.marcotulio.financeiro.cartao.FaturaRegistrada;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * As regras de um lançamento que não dependem do banco (RF01, RF13, RN02, ADR-005).
 *
 * <p>São funções puras: recebem a categoria e a fatura já buscadas pelo
 * {@link ServicoLancamentos} e só decidem. Por isso são testadas sem Spring e sem MySQL.
 */
final class RegrasDoLancamento {

    /** O tamanho da coluna {@code lancamento.descricao}. */
    static final int TAMANHO_MAXIMO_DESCRICAO = 200;

    private static final DateTimeFormatter MES_E_ANO = DateTimeFormatter.ofPattern("MM/yyyy");

    private RegrasDoLancamento() {
    }

    /**
     * Confere o que não pode faltar, antes de o serviço buscar a categoria e a fatura. O "para
     * quem" não tem valor padrão: o sistema nunca supõe a quem o lançamento se destina (RF13).
     */
    static void exigirCamposObrigatorios(DadosDoLancamento dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Informe os dados do lançamento.");
        }
        exigir(dados.tipo() != null, "Informe se é receita ou despesa.");
        exigir(dados.valor() != null, "Informe o valor.");
        exigir(dados.descricao() != null && !dados.descricao().isBlank(), "Informe a descrição.");
        exigir(dados.categoriaId() != null, "Informe a categoria.");
        exigir(dados.destinatarioId() != null, "Informe para quem é o lançamento.");
        exigir(dados.dataCompra() != null, "Informe a data da compra.");
    }

    /**
     * Confere os dados e decide o mês de referência.
     *
     * @param dados já aprovados por {@link #exigirCamposObrigatorios}
     * @param categoria a categoria de {@code dados.categoriaId()}
     * @param fatura a fatura do cartão em que a compra entra, ou {@code null} sem cartão
     */
    static LancamentoConferido conferir(
            DadosDoLancamento dados, CategoriaCadastrada categoria, FaturaRegistrada fatura) {
        exigirCategoriaDoMesmoTipo(dados.tipo(), categoria);
        YearMonth mesReferencia = mesReferencia(dados.dataCompra(), dados.mesReferenciaInformado(), fatura);
        return new LancamentoConferido(
                dados.tipo(),
                valorEmCentavos(dados.valor()),
                descricao(dados.descricao()),
                categoria.id(),
                dados.destinatarioId(),
                dados.dataCompra(),
                mesReferencia,
                dados.cartaoId(),
                faturaQueAcompanhaOMes(fatura, mesReferencia));
    }

    /**
     * O valor com escala 2 (ADR-005). Uma fração de centavo é recusada, e não arredondada:
     * arredondar em silêncio mudaria o valor que o usuário digitou. Por isso o
     * {@code RoundingMode.UNNECESSARY}: depois da conferência, não sobra nada a arredondar.
     */
    private static BigDecimal valorEmCentavos(BigDecimal valor) {
        exigir(valor.signum() > 0, "O valor deve ser maior que zero.");
        // stripTrailingZeros: "150.500" tem escala 3, mas são só R$ 150,50.
        exigir(valor.stripTrailingZeros().scale() <= 2, "O valor aceita no máximo dois decimais (centavos).");
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * O limite é contado em caracteres, como faz o MySQL, e não em posições da {@code String}:
     * em Java, um emoji ocupa duas posições ({@code codePointCount} conta um).
     */
    private static String descricao(String descricao) {
        String semEspacosNasPontas = descricao.strip();
        exigir(semEspacosNasPontas.codePointCount(0, semEspacosNasPontas.length()) <= TAMANHO_MAXIMO_DESCRICAO,
                "A descrição aceita no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres.");
        return semEspacosNasPontas;
    }

    private static void exigirCategoriaDoMesmoTipo(TipoLancamento tipo, CategoriaCadastrada categoria) {
        exigir(tipo.combinaCom(categoria.tipo()),
                "A categoria " + categoria.nome() + " é de " + nomeDoTipo(categoria.tipo())
                        + " e não pode classificar uma " + nomeDoTipo(tipo) + ".");
    }

    /**
     * RF01: sem cartão, o mês da compra; com cartão, o mês da fatura; informado, prevalece.
     * RN02: em nenhum caso o mês de referência é anterior ao mês da compra.
     */
    private static YearMonth mesReferencia(LocalDate dataCompra, YearMonth informado, FaturaRegistrada fatura) {
        YearMonth mesDaCompra = YearMonth.from(dataCompra);
        YearMonth calculado = fatura == null ? mesDaCompra : fatura.mesReferencia();
        YearMonth escolhido = informado == null ? calculado : informado;
        exigir(!escolhido.isBefore(mesDaCompra),
                "O mês de referência (" + escolhido.format(MES_E_ANO)
                        + ") não pode ser anterior ao mês da compra (" + mesDaCompra.format(MES_E_ANO) + ").");
        return escolhido;
    }

    /**
     * O lançamento fica na fatura só quando o mês de referência é o da fatura. O modelo de dados
     * (seção 3.7) exige que os dois meses nunca divirjam; se o usuário informou outro mês, a
     * compra continua ligada ao cartão, mas a nenhuma fatura. Movê-la entre faturas é assunto da
     * revisão de fatura (RF15).
     */
    private static Long faturaQueAcompanhaOMes(FaturaRegistrada fatura, YearMonth mesReferencia) {
        if (fatura == null || !fatura.mesReferencia().equals(mesReferencia)) {
            return null;
        }
        return fatura.id();
    }

    private static String nomeDoTipo(TipoLancamento tipo) {
        return tipo == TipoLancamento.RECEITA ? "receita" : "despesa";
    }

    private static String nomeDoTipo(TipoCategoria tipo) {
        return tipo == TipoCategoria.RECEITA ? "receita" : "despesa";
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
