package com.marcotulio.financeiro.planejamento;

import com.marcotulio.financeiro.cadastro.CategoriaCadastrada;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.planejamento.DadosIniciaisDoPlanejamento.MetaEconomiaInicial;
import com.marcotulio.financeiro.planejamento.DadosIniciaisDoPlanejamento.OrcamentoInicial;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Carrega o orçamento e a meta de economia iniciais do arquivo de dados iniciais toda vez que a
 * aplicação sobe.
 *
 * <p>Roda depois da carga do cadastro ({@code @Order(3)}), porque cada orçamento aponta para uma
 * categoria. Como nas outras cargas, é idempotente e, sem o arquivo, nada é carregado.
 */
@Component
@Order(3)
class CargaPlanejamentoInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaPlanejamentoInicial.class);

    private final ServicoPlanejamento planejamento;
    private final ServicoCategorias categorias;
    private final Resource arquivo;

    CargaPlanejamentoInicial(
            ServicoPlanejamento planejamento,
            ServicoCategorias categorias,
            @Value("${financeiro.dados-iniciais.arquivo}") Resource arquivo) {
        this.planejamento = planejamento;
        this.categorias = categorias;
        this.arquivo = arquivo;
    }

    @Override
    public void run(ApplicationArguments argumentos) throws IOException {
        carregar(arquivo);
    }

    void carregar(Resource arquivo) throws IOException {
        if (!arquivo.exists()) {
            LOG.warn("Arquivo de dados iniciais não encontrado ({}); nenhum orçamento foi carregado.",
                    arquivo.getDescription());
            return;
        }

        DadosIniciaisDoPlanejamento dados = ler(arquivo);

        for (OrcamentoInicial orcamento : dados.orcamentos()) {
            CategoriaCadastrada categoria = categorias.buscarPorNomeETipo(orcamento.categoria(), orcamento.tipo())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Orçamento de uma categoria que não existe: " + orcamento.categoria()
                                    + " (" + orcamento.tipo() + ")."));
            planejamento.definirOrcamentoSeAusente(
                    categoria.id(), orcamento.vigenteDesde(), orcamento.valorPlanejado());
        }
        for (MetaEconomiaInicial meta : dados.metasEconomia()) {
            planejamento.definirMetaDeEconomiaSeAusente(meta.vigenteDesde(), meta.percentual());
        }

        // Só as quantidades: valores do domicílio não vão para o log.
        LOG.info("Dados iniciais conferidos: {} orçamentos e {} metas de economia no arquivo.",
                dados.orcamentos().size(), dados.metasEconomia().size());
    }

    /** Lê o YAML do mesmo jeito que a carga do cadastro ({@code CargaCadastrosIniciais}). */
    private static DadosIniciaisDoPlanejamento ler(Resource arquivo) throws IOException {
        var conteudo = new YamlPropertySourceLoader().load("dados-iniciais", arquivo);
        Binder binder = new Binder(ConfigurationPropertySources.from(conteudo));
        return binder.bindOrCreate("", DadosIniciaisDoPlanejamento.class);
    }
}
