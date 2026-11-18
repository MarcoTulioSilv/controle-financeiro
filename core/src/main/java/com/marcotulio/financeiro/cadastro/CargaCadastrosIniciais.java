package com.marcotulio.financeiro.cadastro;

import com.marcotulio.financeiro.cadastro.DadosIniciaisDoCadastro.CategoriaInicial;
import com.marcotulio.financeiro.cadastro.DadosIniciaisDoCadastro.DestinatarioInicial;
import com.marcotulio.financeiro.cadastro.DadosIniciaisDoCadastro.UsuarioInicial;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Carrega destinatários, categorias, palavras-chave e usuários do arquivo de dados iniciais toda vez que a
 * aplicação sobe.
 *
 * <p>O arquivo real tem dados do domicílio e por isso fica fora do Git; se ele não existir, nada
 * é carregado. A carga é idempotente: o que já está no banco não é duplicado nem alterado, então
 * rodar de novo é seguro e só acrescenta o que for novo no arquivo.
 */
@Component
class CargaCadastrosIniciais implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaCadastrosIniciais.class);

    private final ServicoDestinatarios destinatarios;
    private final ServicoCategorias categorias;
    private final ServicoUsuarios usuarios;
    private final Resource arquivo;

    CargaCadastrosIniciais(
            ServicoDestinatarios destinatarios,
            ServicoCategorias categorias,
            ServicoUsuarios usuarios,
            @Value("${financeiro.dados-iniciais.arquivo}") Resource arquivo) {
        this.destinatarios = destinatarios;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.arquivo = arquivo;
    }

    @Override
    public void run(ApplicationArguments argumentos) throws IOException {
        carregar(arquivo);
    }

    void carregar(Resource arquivo) throws IOException {
        if (!arquivo.exists()) {
            LOG.warn("Arquivo de dados iniciais não encontrado ({}); nada foi carregado.",
                    arquivo.getDescription());
            return;
        }

        DadosIniciaisDoCadastro dados = ler(arquivo);

        for (DestinatarioInicial destinatario : dados.destinatarios()) {
            destinatarios.cadastrarSeAusente(destinatario.nome(), destinatario.marcacaoBot());
        }
        for (CategoriaInicial categoria : dados.categorias()) {
            CategoriaCadastrada gravada = categorias.cadastrarSeAusente(
                    categoria.nome(), categoria.tipo(), categoria.ativa());
            for (String palavraChave : categoria.palavrasChave()) {
                categorias.associarPalavraChaveSeAusente(palavraChave, gravada.id());
            }
        }
        // Depois dos destinatários, porque cada usuário aponta para um deles.
        for (UsuarioInicial usuario : dados.usuarios()) {
            usuarios.cadastrarSeAusente(
                    usuario.nome(), usuario.login(), usuario.senhaInicial(), usuario.destinatario());
        }

        // Só as quantidades: nomes do domicílio não vão para o log.
        LOG.info("Dados iniciais conferidos: {} destinatários, {} categorias e {} usuários no arquivo.",
                dados.destinatarios().size(), dados.categorias().size(), dados.usuarios().size());
    }

    /**
     * Lê o YAML com as mesmas ferramentas que o Spring Boot usa no {@code application.yaml}, sem
     * misturar estes dados às configurações da aplicação. Um campo inválido (por exemplo, um tipo
     * de categoria que não existe) interrompe a carga antes de qualquer gravação.
     */
    private static DadosIniciaisDoCadastro ler(Resource arquivo) throws IOException {
        var conteudo = new YamlPropertySourceLoader().load("dados-iniciais", arquivo);
        Binder binder = new Binder(ConfigurationPropertySources.from(conteudo));
        return binder.bindOrCreate("", DadosIniciaisDoCadastro.class);
    }
}
