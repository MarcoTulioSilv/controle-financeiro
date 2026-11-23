package com.marcotulio.financeiro.cadastro;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública do cadastro de categorias e de suas palavras-chave (RF02). */
@Service
public class ServicoCategorias {

    private final RepositorioDeCategorias categorias;
    private final RepositorioDePalavrasChave palavrasChave;

    ServicoCategorias(RepositorioDeCategorias categorias, RepositorioDePalavrasChave palavrasChave) {
        this.categorias = categorias;
        this.palavrasChave = palavrasChave;
    }

    /**
     * Cadastra a categoria, a menos que já exista uma com o mesmo nome e tipo: nesse caso devolve
     * a que está gravada, sem alterá-la (nem o {@code ativa}). Assim a carga de dados iniciais
     * pode rodar de novo sem desfazer o que o usuário mudou depois.
     */
    @Transactional
    public CategoriaCadastrada cadastrarSeAusente(String nome, TipoCategoria tipo, boolean ativa) {
        Categoria categoria = categorias.findByNomeAndTipo(nome, tipo)
                .orElseGet(() -> categorias.save(new Categoria(nome, tipo, ativa)));
        return categoria.paraConsulta();
    }

    /** As opções oferecidas para novos lançamentos: as inativas ficam de fora (RF02). */
    @Transactional(readOnly = true)
    public List<CategoriaCadastrada> listarAtivas(TipoCategoria tipo) {
        return categorias.findByTipoAndAtivaTrueOrderByNome(tipo).stream()
                .map(Categoria::paraConsulta)
                .toList();
    }

    /**
     * A categoria com o id informado, ativa ou não. Usado pelo lançamento para conferir se a
     * categoria é do mesmo tipo (receita ou despesa) que ele.
     */
    @Transactional(readOnly = true)
    public Optional<CategoriaCadastrada> buscarPorId(long categoriaId) {
        return categorias.findById(categoriaId).map(Categoria::paraConsulta);
    }

    /**
     * A categoria com o nome e o tipo informados, ativa ou não, ignorando acentos e maiúsculas
     * (collation C3). Usado pelas dívidas, porque toda parcela vai para a categoria Dívidas
     * (RF12), e pela carga do orçamento, que identifica a categoria pelo nome no arquivo de
     * dados iniciais.
     */
    @Transactional(readOnly = true)
    public Optional<CategoriaCadastrada> buscarPorNomeETipo(String nome, TipoCategoria tipo) {
        return categorias.findByNomeAndTipo(nome, tipo).map(Categoria::paraConsulta);
    }

    /** Categorias nunca são excluídas: são desativadas e continuam nos lançamentos antigos. */
    @Transactional
    public void desativar(long categoriaId) {
        buscar(categoriaId).desativar();
    }

    /**
     * Associa a palavra-chave à categoria, a menos que a palavra já exista. Como uma palavra
     * pertence a uma única categoria, uma palavra já associada continua onde estava.
     */
    @Transactional
    public void associarPalavraChaveSeAusente(String texto, long categoriaId) {
        Categoria categoria = buscar(categoriaId);
        if (!palavrasChave.existsByTexto(texto)) {
            palavrasChave.save(new PalavraChave(texto, categoria));
        }
    }

    /**
     * Se o texto já é uma palavra-chave, ignorando acentos e maiúsculas (collation C3). Usado pelo
     * cadastro de cartões: o apelido de um cartão não pode ser também uma palavra-chave (RF14).
     */
    @Transactional(readOnly = true)
    public boolean existePalavraChave(String texto) {
        return palavrasChave.existsByTexto(texto);
    }

    private Categoria buscar(long categoriaId) {
        return categorias.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria não encontrada: " + categoriaId));
    }
}
