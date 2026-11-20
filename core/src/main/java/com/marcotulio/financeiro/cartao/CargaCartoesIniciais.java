package com.marcotulio.financeiro.cartao;

import com.marcotulio.financeiro.cartao.DadosIniciaisDeCartoes.CartaoInicial;
import com.marcotulio.financeiro.cartao.DadosIniciaisDeCartoes.FeriadoLocalInicial;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
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
 * Grava, toda vez que a aplicação sobe, os feriados nacionais do ano corrente e do seguinte, e
 * carrega os cartões e os feriados locais do arquivo de dados iniciais.
 *
 * <p>Roda depois da carga do cadastro ({@code @Order(2)}), porque cada cartão aponta para um
 * usuário titular e o apelido é conferido contra as palavras-chave e as marcações. Como lá, a
 * carga é idempotente e, sem o arquivo, os cartões simplesmente não são carregados.
 */
@Component
@Order(2)
class CargaCartoesIniciais implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaCartoesIniciais.class);

    private final ServicoCartoes cartoes;
    private final ServicoFeriados feriados;
    private final Clock relogio;
    private final Resource arquivo;

    CargaCartoesIniciais(
            ServicoCartoes cartoes,
            ServicoFeriados feriados,
            Clock relogio,
            @Value("${financeiro.dados-iniciais.arquivo}") Resource arquivo) {
        this.cartoes = cartoes;
        this.feriados = feriados;
        this.relogio = relogio;
        this.arquivo = arquivo;
    }

    @Override
    public void run(ApplicationArguments argumentos) throws IOException {
        carregar(arquivo);
    }

    void carregar(Resource arquivo) throws IOException {
        // Os nacionais não dependem do arquivo: vêm do CalendarioBancario.
        int anoCorrente = LocalDate.now(relogio).getYear();
        feriados.gravarFeriadosNacionais(anoCorrente);
        feriados.gravarFeriadosNacionais(anoCorrente + 1);

        if (!arquivo.exists()) {
            LOG.warn("Arquivo de dados iniciais não encontrado ({}); nenhum cartão foi carregado.",
                    arquivo.getDescription());
            return;
        }

        DadosIniciaisDeCartoes dados = ler(arquivo);

        for (FeriadoLocalInicial feriado : dados.feriadosLocais()) {
            feriados.cadastrarLocalSeAusente(feriado.data(), feriado.descricao());
        }
        for (CartaoInicial cartao : dados.cartoes()) {
            cartoes.cadastrarSeAusente(cartao.apelido(), cartao.nome(), cartao.titular(), cartao.regra());
        }

        LOG.info("Dados iniciais conferidos: {} cartões e {} feriados locais no arquivo.",
                dados.cartoes().size(), dados.feriadosLocais().size());
    }

    /** Lê o YAML do mesmo jeito que a carga do cadastro ({@code CargaCadastrosIniciais}). */
    private static DadosIniciaisDeCartoes ler(Resource arquivo) throws IOException {
        var conteudo = new YamlPropertySourceLoader().load("dados-iniciais", arquivo);
        Binder binder = new Binder(ConfigurationPropertySources.from(conteudo));
        return binder.bindOrCreate("", DadosIniciaisDeCartoes.class);
    }
}
