package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga de cartões e feriados a partir do arquivo de dados iniciais.
 *
 * <p>Como no cadastro, os testes usam só o exemplo versionado, com os nomes fictícios do ERS
 * (Apêndice B), nunca o arquivo real. Os titulares são cadastrados antes, pelo serviço público do
 * cadastro, como acontece na aplicação: a carga do cadastro roda primeiro.
 */
@Transactional
class CargaCartoesIniciaisTest extends TesteDeIntegracao {

    private static final Resource EXEMPLO =
            new FileSystemResource("../config/dados-iniciais.example.yml");

    @Autowired
    private CargaCartoesIniciais carga;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private Clock relogio;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void cadastraOsTitulares() {
        destinatarios.cadastrarSeAusente("Marco", "@mt");
        destinatarios.cadastrarSeAusente("Ana", "@ana");
        usuarios.cadastrarSeAusente("Marco", "marco", "senha-de-teste", "@mt");
        usuarios.cadastrarSeAusente("Ana", "ana", "senha-de-teste", "@ana");
    }

    @Test
    void carregaOsCincoCartoesDoApendiceB() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert
        var cartoes = jdbc.sql("""
                        SELECT c.apelido, c.nome, u.login, c.dia_fechamento, c.dia_vencimento,
                               c.compra_no_fechamento
                          FROM cartao c JOIN usuario u ON u.id = c.titular_id
                        """)
                .query((linha, numero) -> String.join(" ",
                        linha.getString("apelido"), linha.getString("nome"), linha.getString("login"),
                        linha.getString("dia_fechamento"), linha.getString("dia_vencimento"),
                        linha.getString("compra_no_fechamento")))
                .list();
        assertThat(cartoes).containsExactlyInAnyOrder(
                "amt Banco A marco 20 25 PROXIMA",
                "aana Banco A ana 10 15 PROXIMA",
                "bmt Banco B marco 5 12 PROXIMA",
                "cmt Banco C marco 15 25 PROXIMA",
                "cana Banco C ana 15 25 PROXIMA");
    }

    @Test
    void gravaOsFeriadosNacionaisDoAnoCorrenteEDoSeguinte() throws Exception {
        // Arrange
        int anoCorrente = LocalDate.now(relogio).getYear();
        CalendarioBancario calendario = new CalendarioBancario();

        // Act
        carga.carregar(EXEMPLO);

        // Assert
        assertThat(datasNacionaisDoAno(anoCorrente))
                .containsExactlyInAnyOrderElementsOf(calendario.feriadosNacionais(anoCorrente).keySet());
        assertThat(datasNacionaisDoAno(anoCorrente + 1))
                .containsExactlyInAnyOrderElementsOf(calendario.feriadosNacionais(anoCorrente + 1).keySet());
    }

    @Test
    void carregaOsFeriadosLocaisDoExemplo() throws Exception {
        // Act
        carga.carregar(EXEMPLO);

        // Assert
        var locais = jdbc.sql("SELECT data FROM feriado WHERE abrangencia = 'LOCAL'")
                .query(LocalDate.class)
                .list();
        assertThat(locais).containsExactlyInAnyOrder(
                LocalDate.of(2026, 7, 15), LocalDate.of(2027, 7, 15));
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
    void semArquivoGravaSoOsFeriadosNacionais() throws Exception {
        // Arrange
        int anoCorrente = LocalDate.now(relogio).getYear();

        // Act
        carga.carregar(new FileSystemResource("../config/arquivo-que-nao-existe.yml"));

        // Assert
        assertThat(datasNacionaisDoAno(anoCorrente)).isNotEmpty();
        assertThat(contar("SELECT COUNT(*) FROM cartao")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM feriado WHERE abrangencia = 'LOCAL'")).isZero();
    }

    @Test
    void cartaoComTitularQueNaoEhUsuarioInterrompeACarga() {
        // Arrange
        Resource invalido = new ByteArrayResource("""
                cartoes:
                  - apelido: xmt
                    nome: Banco X
                    titular: ninguem
                    dia-fechamento: 20
                    dia-vencimento: 25
                    compra-no-fechamento: PROXIMA
                """.getBytes(StandardCharsets.UTF_8));

        // Act e Assert
        assertThatThrownBy(() -> carga.carregar(invalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninguem");
    }

    private List<LocalDate> datasNacionaisDoAno(int ano) {
        return jdbc.sql("SELECT data FROM feriado WHERE abrangencia = 'NACIONAL' AND YEAR(data) = ?")
                .param(ano)
                .query(LocalDate.class)
                .list();
    }

    /** Total de linhas de cartao e feriado, nessa ordem. */
    private int[] contarTudo() {
        return new int[] {contar("SELECT COUNT(*) FROM cartao"), contar("SELECT COUNT(*) FROM feriado")};
    }

    private int contar(String consulta) {
        return jdbc.sql(consulta).query(Integer.class).single();
    }
}
