package com.marcotulio.financeiro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restrições das tabelas do bloco A ({@code docs/design/modelo-dados.md}) garantidas pelo próprio
 * banco: mesmo que um código futuro esqueça uma validação, o MySQL recusa o dado inválido.
 *
 * <p>Os testes gravam com SQL direto, sem passar pelas entidades, justamente para provar que a
 * regra está no schema. {@code @Transactional} desfaz cada teste ao final, e por isso os nomes
 * usados aqui podem se repetir entre os testes.
 */
@Transactional
class RestricoesDoCadastroTest extends TesteDeIntegracao {

    @Autowired
    private JdbcClient jdbc;

    // ----- destinatario -----

    @Test
    void destinatarioNaoAceitaNomeRepetido() {
        // Arrange
        inserirDestinatario("Visitante", "@vis");

        // Act e Assert
        assertThatThrownBy(() -> inserirDestinatario("Visitante", "@outro"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void destinatarioNaoAceitaMarcacaoRepetidaNemComOutraCaixa() {
        // Arrange
        inserirDestinatario("Visitante", "@vis");

        // Act e Assert: pela collation, @VIS e @vis são a mesma marcação.
        assertThatThrownBy(() -> inserirDestinatario("Outro visitante", "@VIS"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void destinatarioNasceAtivo() {
        // Arrange
        inserirDestinatario("Visitante", "@vis");

        // Act
        Boolean ativo = jdbc.sql("SELECT ativo FROM destinatario WHERE marcacao_bot = '@vis'")
                .query(Boolean.class)
                .single();

        // Assert
        assertThat(ativo).isTrue();
    }

    // ----- categoria -----

    @Test
    void categoriaSoAceitaReceitaOuDespesa() {
        // Act e Assert
        assertThatThrownBy(() -> inserirCategoria("Teste", "TRANSFERENCIA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void categoriaNaoRepeteONomeDentroDoMesmoTipo() {
        // Arrange
        inserirCategoria("Teste", "DESPESA");

        // Act e Assert
        assertThatThrownBy(() -> inserirCategoria("Teste", "DESPESA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void categoriaPodeTerOMesmoNomeEmTiposDiferentes() {
        // Arrange: é o caso de "Investimentos" no Apêndice A do ERS.
        inserirCategoria("Teste", "DESPESA");

        // Act
        inserirCategoria("Teste", "RECEITA");

        // Assert
        Integer total = jdbc.sql("SELECT COUNT(*) FROM categoria WHERE nome = 'Teste'")
                .query(Integer.class)
                .single();
        assertThat(total).isEqualTo(2);
    }

    @Test
    void categoriaNasceAtiva() {
        // Arrange
        long id = inserirCategoria("Teste", "DESPESA");

        // Act
        Boolean ativa = jdbc.sql("SELECT ativa FROM categoria WHERE id = ?")
                .param(id)
                .query(Boolean.class)
                .single();

        // Assert
        assertThat(ativa).isTrue();
    }

    // ----- palavra_chave -----

    @Test
    void palavraChaveComAcentoEMaiusculaEhAMesmaPalavraSemEles() {
        // Arrange
        long categoria = inserirCategoria("Teste", "DESPESA");
        inserirPalavraChave("Farmácia", categoria);

        // Act e Assert: a collation utf8mb4_0900_ai_ci ignora acentos e maiúsculas (C3).
        assertThatThrownBy(() -> inserirPalavraChave("farmacia", categoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void palavraChaveEhEncontradaSemAcentoNemMaiuscula() {
        // Arrange
        long categoria = inserirCategoria("Teste", "DESPESA");
        inserirPalavraChave("Farmácia", categoria);

        // Act
        String encontrada = jdbc.sql("SELECT texto FROM palavra_chave WHERE texto = 'farmacia'")
                .query(String.class)
                .single();

        // Assert
        assertThat(encontrada).isEqualTo("Farmácia");
    }

    @Test
    void palavraChavePertenceAUmaUnicaCategoria() {
        // Arrange
        long umaCategoria = inserirCategoria("Teste", "DESPESA");
        long outraCategoria = inserirCategoria("Outro teste", "DESPESA");
        inserirPalavraChave("xpto", umaCategoria);

        // Act e Assert
        assertThatThrownBy(() -> inserirPalavraChave("xpto", outraCategoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void palavraChaveExigeCategoriaExistente() {
        // Act e Assert
        assertThatThrownBy(() -> inserirPalavraChave("xpto", -1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void categoriaComPalavraChaveNaoPodeSerApagada() {
        // Arrange
        long categoria = inserirCategoria("Teste", "DESPESA");
        inserirPalavraChave("xpto", categoria);

        // Act e Assert: a chave estrangeira impede a exclusão (RF02: categorias são desativadas).
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM categoria WHERE id = ?").param(categoria).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- usuario -----

    @Test
    void usuarioNaoAceitaLoginRepetidoNemComOutraCaixa() {
        // Arrange
        long destinatario = inserirDestinatarioERetornarId("Visitante", "@vis");
        inserirUsuario("Visitante", "visitante", destinatario);

        // Act e Assert: pela collation, VISITANTE e visitante são o mesmo login.
        assertThatThrownBy(() -> inserirUsuario("Outro", "VISITANTE", destinatario))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usuarioExigeDestinatarioExistente() {
        // Act e Assert
        assertThatThrownBy(() -> inserirUsuario("Visitante", "visitante", -1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- charset e collation (C3) -----

    @Test
    void asTabelasDoCadastroUsamUtf8mb4ComCollationQueIgnoraAcentos() {
        // Act
        var collations = jdbc.sql("""
                        SELECT table_collation FROM information_schema.tables
                         WHERE table_schema = DATABASE()
                           AND table_name IN ('destinatario', 'categoria', 'palavra_chave', 'usuario')
                        """)
                .query(String.class)
                .list();

        // Assert
        assertThat(collations).hasSize(4).containsOnly("utf8mb4_0900_ai_ci");
    }

    // ----- auxiliares -----

    private void inserirDestinatario(String nome, String marcacaoBot) {
        jdbc.sql("""
                        INSERT INTO destinatario (nome, marcacao_bot, %s)
                        VALUES (?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(nome, marcacaoBot)
                .update();
    }

    private long inserirDestinatarioERetornarId(String nome, String marcacaoBot) {
        inserirDestinatario(nome, marcacaoBot);
        return ultimoIdGerado();
    }

    private void inserirUsuario(String nome, String login, long destinatarioId) {
        jdbc.sql("""
                        INSERT INTO usuario (nome, login, senha_hash, destinatario_id, %s)
                        VALUES (?, ?, '{bcrypt}hash-de-teste', ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(nome, login, destinatarioId)
                .update();
    }

    private long inserirCategoria(String nome, String tipo) {
        jdbc.sql("""
                        INSERT INTO categoria (nome, tipo, %s)
                        VALUES (?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(nome, tipo)
                .update();
        return ultimoIdGerado();
    }

    private void inserirPalavraChave(String texto, long categoriaId) {
        jdbc.sql("""
                        INSERT INTO palavra_chave (texto, categoria_id, %s)
                        VALUES (?, ?, %s)
                        """.formatted(COLUNAS_C7, VALORES_C7))
                .params(texto, categoriaId)
                .update();
    }

    private long ultimoIdGerado() {
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    /** Colunas padrão de auditoria e versão (C7), que todo INSERT precisa preencher. */
    private static final String COLUNAS_C7 =
            "criado_em, criado_por, atualizado_em, atualizado_por, versao";

    private static final String VALORES_C7 =
            "UTC_TIMESTAMP(3), 'teste', UTC_TIMESTAMP(3), 'teste', 0";
}
