package com.marcotulio.financeiro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga de destinatários, categorias e palavras-chave a partir do arquivo de dados iniciais.
 *
 * <p>Os testes nunca leem o arquivo real ({@code config/dados-iniciais.yml}): usam o exemplo
 * versionado, que só tem os nomes fictícios do ERS. Assim o teste também garante que o exemplo
 * continua válido. O caminho é relativo à pasta {@code core/}, onde o Maven roda os testes.
 */
@Transactional
class CargaCadastrosIniciaisTest extends TesteDeIntegracao {

    private static final Resource EXEMPLO =
            new FileSystemResource("../config/dados-iniciais.example.yml");

    @Autowired
    private CargaCadastrosIniciais carga;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void carregaOsDestinatariosDoExemplo() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(jdbc.sql("SELECT marcacao_bot FROM destinatario").query(String.class).list())
                .containsExactlyInAnyOrder("@mt", "@ana", "@casa");
    }

    @Test
    void carregaAsCategoriasDoApendiceA() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert: 16 de despesa e 9 de receita (as 8 do Apêndice A mais Empresa Anterior).
        assertThat(contarCategorias("DESPESA")).isEqualTo(16);
        assertThat(contarCategorias("RECEITA")).isEqualTo(9);
    }

    @Test
    void empresaAnteriorChegaInativa() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert
        Boolean ativa = jdbc.sql("SELECT ativa FROM categoria WHERE nome = 'Empresa Anterior'")
                .query(Boolean.class)
                .single();
        assertThat(ativa).isFalse();
    }

    @Test
    void palavraChaveLevaACategoriaMesmoSemAcento() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert: o exemplo grava "farmácia"; a busca por "farmacia" a encontra (C3).
        String categoria = jdbc.sql("""
                        SELECT c.nome FROM palavra_chave p JOIN categoria c ON c.id = p.categoria_id
                         WHERE p.texto = 'farmacia'
                        """)
                .query(String.class)
                .single();
        assertThat(categoria).isEqualTo("Saúde/Farmácia");
    }

    @Test
    void rodarDuasVezesNaoDuplicaNada() throws Exception {
        // Arrange
        carga.carregar(EXEMPLO);
        int[] depoisDaPrimeira = contarTudo();

        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(contarTudo()).containsExactly(depoisDaPrimeira);
    }

    @Test
    void semArquivoNaoCarregaNadaENaoFalha() throws Exception {
        // Arrange
        int[] antes = contarTudo();

        // Act
        carga.carregar(new FileSystemResource("../config/arquivo-que-nao-existe.yml"));

        // Assert
        assertThat(contarTudo()).containsExactly(antes);
    }

    @Test
    void arquivoComTipoInvalidoInterrompeACargaSemGravarNada() {
        // Arrange: o destinatário vem antes no arquivo, mas não pode ser gravado.
        Resource invalido = new ByteArrayResource("""
                destinatarios:
                  - nome: Visitante
                    marcacao-bot: "@vis"
                categorias:
                  - nome: Categoria de teste
                    tipo: TRANSFERENCIA
                """.getBytes(StandardCharsets.UTF_8));
        int[] antes = contarTudo();

        // Act e Assert
        assertThatThrownBy(() -> carga.carregar(invalido)).isInstanceOf(BindException.class);
        assertThat(contarTudo()).containsExactly(antes);
    }

    private int contarCategorias(String tipo) {
        return jdbc.sql("SELECT COUNT(*) FROM categoria WHERE tipo = ?")
                .param(tipo)
                .query(Integer.class)
                .single();
    }

    /** Total de linhas de destinatario, categoria e palavra_chave, nessa ordem. */
    private int[] contarTudo() {
        return new int[] {
            contar("SELECT COUNT(*) FROM destinatario"),
            contar("SELECT COUNT(*) FROM categoria"),
            contar("SELECT COUNT(*) FROM palavra_chave")
        };
    }

    private int contar(String consulta) {
        return jdbc.sql(consulta).query(Integer.class).single();
    }
}
