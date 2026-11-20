package com.marcotulio.financeiro.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcotulio.financeiro.TesteDeIntegracao;
import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import com.marcotulio.financeiro.cadastro.TipoCategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de cartões de crédito (RF14). */
@Transactional
class ServicoCartoesTest extends TesteDeIntegracao {

    private static final RegraCartao FECHA_5_VENCE_12 =
            new RegraCartao(5, 12, CompraNoFechamento.PROXIMA);

    @Autowired
    private ServicoCartoes servico;

    @Autowired
    private ServicoDestinatarios destinatarios;

    @Autowired
    private ServicoUsuarios usuarios;

    @Autowired
    private ServicoCategorias categorias;

    @Autowired
    private JdbcClient jdbc;

    private long marco;

    @BeforeEach
    void cadastraOTitular() {
        destinatarios.cadastrarSeAusente("Marco", "@mt");
        usuarios.cadastrarSeAusente("Marco", "marco", "senha-de-teste", "@mt");
        marco = usuarios.buscarIdPeloLogin("marco").orElseThrow();
    }

    @Test
    void cadastraCartaoAtivoComORegraEOTitular() {
        // Act
        CartaoCadastrado cartao = servico.cadastrarSeAusente("bmt", "Banco B", "marco", FECHA_5_VENCE_12);

        // Assert
        assertThat(cartao.id()).isPositive();
        assertThat(cartao.apelido()).isEqualTo("bmt");
        assertThat(cartao.nome()).isEqualTo("Banco B");
        assertThat(cartao.titularId()).isEqualTo(marco);
        assertThat(cartao.regra()).isEqualTo(FECHA_5_VENCE_12);
        assertThat(cartao.ativo()).isTrue();
    }

    @Test
    void naoDuplicaNemAlteraCartaoComOMesmoApelido() {
        // Arrange
        CartaoCadastrado primeiro = servico.cadastrarSeAusente("bmt", "Banco B", "marco", FECHA_5_VENCE_12);

        // Act: rodar a carga de novo não desfaz o que foi mudado depois.
        CartaoCadastrado segundo = servico.cadastrarSeAusente(
                "BMT", "Outro nome", "marco", new RegraCartao(10, 20, CompraNoFechamento.ATUAL));

        // Assert
        assertThat(segundo).isEqualTo(primeiro);
        assertThat(contarCartoes()).isEqualTo(1);
    }

    @Test
    void recusaApelidoIgualAUmaPalavraChaveMesmoComAcentoEMaiuscula() {
        // Arrange
        var alimentacao = categorias.cadastrarSeAusente("Alimentação", TipoCategoria.DESPESA, true);
        categorias.associarPalavraChaveSeAusente("açaí", alimentacao.id());

        // Act e Assert: "acai 30 acai @mt" seria ambíguo para o bot.
        assertThatThrownBy(() -> servico.cadastrarSeAusente("ACAI", "Banco A", "marco", FECHA_5_VENCE_12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("palavra-chave");
        assertThat(contarCartoes()).isZero();
    }

    @Test
    void recusaApelidoIgualAUmaMarcacaoDeDestinatario() {
        // Act e Assert
        assertThatThrownBy(() -> servico.cadastrarSeAusente("@mt", "Banco A", "marco", FECHA_5_VENCE_12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marcação");
        assertThat(contarCartoes()).isZero();
    }

    @Test
    void recusaApelidoIgualAUmaMarcacaoSemOArroba() {
        // Act e Assert: "mt" e "@mt" na mesma mensagem confundiriam quem lê.
        assertThatThrownBy(() -> servico.cadastrarSeAusente("MT", "Banco A", "marco", FECHA_5_VENCE_12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marcação");
        assertThat(contarCartoes()).isZero();
    }

    @Test
    void recusaTitularQueNaoEhUsuario() {
        // Act e Assert
        assertThatThrownBy(() -> servico.cadastrarSeAusente("bmt", "Banco B", "ninguem", FECHA_5_VENCE_12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninguem");
        assertThat(contarCartoes()).isZero();
    }

    private int contarCartoes() {
        return jdbc.sql("SELECT COUNT(*) FROM cartao").query(Integer.class).single();
    }
}
