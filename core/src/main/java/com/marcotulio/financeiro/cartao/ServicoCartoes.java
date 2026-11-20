package com.marcotulio.financeiro.cartao;

import com.marcotulio.financeiro.cadastro.ServicoCategorias;
import com.marcotulio.financeiro.cadastro.ServicoDestinatarios;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública do cadastro de cartões de crédito (RF14). */
@Service
public class ServicoCartoes {

    private final RepositorioDeCartoes cartoes;
    private final ServicoUsuarios usuarios;
    private final ServicoCategorias categorias;
    private final ServicoDestinatarios destinatarios;

    ServicoCartoes(
            RepositorioDeCartoes cartoes,
            ServicoUsuarios usuarios,
            ServicoCategorias categorias,
            ServicoDestinatarios destinatarios) {
        this.cartoes = cartoes;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.destinatarios = destinatarios;
    }

    /**
     * Cadastra o cartão, a menos que o apelido já exista: nesse caso devolve o que está gravado,
     * sem alterá-lo. É o que torna a carga de dados iniciais idempotente.
     *
     * @param loginTitular login do usuário titular do cartão
     * @throws IllegalArgumentException se o titular não for usuário, ou se o apelido coincidir com
     *     uma palavra-chave ou com a marcação de um destinatário
     */
    @Transactional
    public CartaoCadastrado cadastrarSeAusente(
            String apelido, String nome, String loginTitular, RegraCartao regra) {
        Optional<Cartao> existente = cartoes.findByApelido(apelido);
        if (existente.isPresent()) {
            return existente.get().paraConsulta();
        }
        exigirApelidoSemConflito(apelido);
        long titularId = usuarios.buscarIdPeloLogin(loginTitular)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Titular do cartão " + apelido + " não é usuário: " + loginTitular + "."));
        return cartoes.save(new Cartao(apelido, nome, titularId, regra)).paraConsulta();
    }

    /**
     * O bot reconhece cartão, categoria e destinatário pelas palavras da mensagem: se o apelido
     * fosse também uma palavra-chave ou uma marcação, a mesma palavra teria dois sentidos. O
     * banco não garante isso, porque o {@code UNIQUE} só atua dentro de uma tabela.
     *
     * <p>A marcação é conferida com e sem o {@code @}: um cartão {@code mt} ao lado do
     * destinatário {@code @mt} confundiria quem escreve a mensagem.
     */
    private void exigirApelidoSemConflito(String apelido) {
        if (categorias.existePalavraChave(apelido)) {
            throw new IllegalArgumentException(
                    "O apelido de cartão " + apelido + " já é uma palavra-chave de categoria.");
        }
        if (destinatarios.existeMarcacao(apelido) || destinatarios.existeMarcacao("@" + apelido)) {
            throw new IllegalArgumentException(
                    "O apelido de cartão " + apelido + " já é a marcação de um destinatário.");
        }
    }
}
