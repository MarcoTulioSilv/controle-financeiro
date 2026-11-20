package com.marcotulio.financeiro.cadastro;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública do cadastro de destinatários (RF13). */
@Service
public class ServicoDestinatarios {

    private final RepositorioDeDestinatarios repositorio;

    ServicoDestinatarios(RepositorioDeDestinatarios repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Cadastra o destinatário, a menos que a marcação já exista: nesse caso devolve o que está
     * gravado, sem alterá-lo. É o que torna a carga de dados iniciais idempotente.
     *
     * <p>Se o nome já pertencer a outra marcação, o {@code UNIQUE} do banco recusa a gravação.
     */
    @Transactional
    public DestinatarioCadastrado cadastrarSeAusente(String nome, String marcacaoBot) {
        Destinatario destinatario = repositorio.findByMarcacaoBot(marcacaoBot)
                .orElseGet(() -> repositorio.save(new Destinatario(nome, marcacaoBot)));
        return destinatario.paraConsulta();
    }

    /**
     * Se a marcação já pertence a um destinatário, sem diferenciar maiúsculas (collation C3).
     * Usado pelo cadastro de cartões: o apelido de um cartão não pode ser uma marcação (RF14).
     */
    @Transactional(readOnly = true)
    public boolean existeMarcacao(String marcacaoBot) {
        return repositorio.findByMarcacaoBot(marcacaoBot).isPresent();
    }
}
