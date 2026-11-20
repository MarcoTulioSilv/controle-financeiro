package com.marcotulio.financeiro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de categorias e de suas palavras-chave (RF02). */
@Transactional
class ServicoCategoriasTest extends TesteDeIntegracao {

    @Autowired
    private ServicoCategorias servico;

    @Autowired
    private JdbcClient jdbc;

    // ----- categorias -----

    @Test
    void cadastraCategoriaAtiva() {
        // Act
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);

        // Assert
        assertThat(categoria.id()).isPositive();
        assertThat(categoria.nome()).isEqualTo("Categoria de teste");
        assertThat(categoria.tipo()).isEqualTo(TipoCategoria.DESPESA);
        assertThat(categoria.ativa()).isTrue();
    }

    @Test
    void cadastraCategoriaJaInativa() {
        // Arrange: é o caso da "Empresa Anterior" (ERS, Apêndice A).
        // Act
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Emprego antigo de teste", TipoCategoria.RECEITA, false);

        // Assert
        assertThat(categoria.ativa()).isFalse();
    }

    @Test
    void naoDuplicaCategoriaComOMesmoNomeETipo() {
        // Arrange
        CategoriaCadastrada primeira =
                servico.cadastrarSeAusente("Saúde de teste", TipoCategoria.DESPESA, true);

        // Act: pela collation, "saude de teste" é o mesmo nome.
        CategoriaCadastrada segunda =
                servico.cadastrarSeAusente("saude de teste", TipoCategoria.DESPESA, true);

        // Assert
        assertThat(segunda.id()).isEqualTo(primeira.id());
        assertThat(segunda.nome()).isEqualTo("Saúde de teste");
    }

    @Test
    void mesmoNomeEmTiposDiferentesSaoDuasCategorias() {
        // Arrange
        CategoriaCadastrada despesa =
                servico.cadastrarSeAusente("Aplicações de teste", TipoCategoria.DESPESA, true);

        // Act
        CategoriaCadastrada receita =
                servico.cadastrarSeAusente("Aplicações de teste", TipoCategoria.RECEITA, true);

        // Assert
        assertThat(receita.id()).isNotEqualTo(despesa.id());
    }

    @Test
    void naoAlteraACategoriaQueJaExiste() {
        // Arrange
        CategoriaCadastrada existente =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);

        // Act: a carga de dados iniciais não desfaz o que o usuário já decidiu.
        CategoriaCadastrada denovo =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, false);

        // Assert
        assertThat(denovo.id()).isEqualTo(existente.id());
        assertThat(denovo.ativa()).isTrue();
    }

    @Test
    void listaSoAsCategoriasAtivasDoTipoPedido() {
        // Arrange
        servico.cadastrarSeAusente("Despesa ativa de teste", TipoCategoria.DESPESA, true);
        servico.cadastrarSeAusente("Despesa inativa de teste", TipoCategoria.DESPESA, false);
        servico.cadastrarSeAusente("Receita ativa de teste", TipoCategoria.RECEITA, true);

        // Act
        var ativas = servico.listarAtivas(TipoCategoria.DESPESA);

        // Assert
        assertThat(ativas).extracting(CategoriaCadastrada::nome)
                .contains("Despesa ativa de teste")
                .doesNotContain("Despesa inativa de teste", "Receita ativa de teste");
    }

    @Test
    void desativarTiraACategoriaDasOpcoesMasNaoAApaga() {
        // Arrange
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);

        // Act
        servico.desativar(categoria.id());

        // Assert
        assertThat(servico.listarAtivas(TipoCategoria.DESPESA))
                .extracting(CategoriaCadastrada::id)
                .doesNotContain(categoria.id());
        Boolean ativaNoBanco = jdbc.sql("SELECT ativa FROM categoria WHERE id = ?")
                .param(categoria.id())
                .query(Boolean.class)
                .single();
        assertThat(ativaNoBanco).isFalse();
    }

    @Test
    void desativarCategoriaInexistenteFalha() {
        // Act e Assert
        assertThatThrownBy(() -> servico.desativar(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void oRepositorioDeCategoriasNaoOfereceExclusao() {
        // Arrange: RF02, categorias são desativadas, nunca excluídas.
        // Act
        var metodosDeExclusao = Arrays.stream(RepositorioDeCategorias.class.getMethods())
                .filter(metodo -> metodo.getName().startsWith("delete"))
                .toList();

        // Assert
        assertThat(metodosDeExclusao).isEmpty();
    }

    // ----- palavras-chave -----

    @Test
    void associaPalavraChaveACategoria() {
        // Arrange
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);

        // Act
        servico.associarPalavraChaveSeAusente("xpto", categoria.id());

        // Assert
        assertThat(categoriaDaPalavra("xpto")).isEqualTo(categoria.id());
    }

    @Test
    void palavraChaveSemAcentoNemMaiusculaNaoDuplica() {
        // Arrange
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);
        servico.associarPalavraChaveSeAusente("Drogaria Teste", categoria.id());

        // Act
        servico.associarPalavraChaveSeAusente("drogaria teste", categoria.id());

        // Assert
        Integer total = jdbc.sql("SELECT COUNT(*) FROM palavra_chave WHERE texto = 'drogaria teste'")
                .query(Integer.class)
                .single();
        assertThat(total).isEqualTo(1);
    }

    @Test
    void palavraChaveJaAssociadaContinuaNaCategoriaOriginal() {
        // Arrange
        CategoriaCadastrada original =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);
        CategoriaCadastrada outra =
                servico.cadastrarSeAusente("Outra categoria de teste", TipoCategoria.DESPESA, true);
        servico.associarPalavraChaveSeAusente("xpto", original.id());

        // Act
        servico.associarPalavraChaveSeAusente("xpto", outra.id());

        // Assert: uma palavra-chave pertence a uma única categoria (RF02).
        assertThat(categoriaDaPalavra("xpto")).isEqualTo(original.id());
    }

    @Test
    void associarPalavraChaveACategoriaInexistenteFalha() {
        // Act e Assert
        assertThatThrownBy(() -> servico.associarPalavraChaveSeAusente("xpto", -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void reconhecePalavraChaveCadastradaSemDiferenciarAcentosNemMaiusculas() {
        // Arrange
        CategoriaCadastrada categoria =
                servico.cadastrarSeAusente("Categoria de teste", TipoCategoria.DESPESA, true);
        servico.associarPalavraChaveSeAusente("Drogaria Teste", categoria.id());

        // Act e Assert
        assertThat(servico.existePalavraChave("drogária teste")).isTrue();
        assertThat(servico.existePalavraChave("xpto")).isFalse();
    }

    private long categoriaDaPalavra(String texto) {
        return jdbc.sql("SELECT categoria_id FROM palavra_chave WHERE texto = ?")
                .param(texto)
                .query(Long.class)
                .single();
    }
}
