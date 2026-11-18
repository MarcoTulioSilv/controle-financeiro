package com.marcotulio.financeiro.cadastro;

import org.springframework.data.repository.Repository;

/** Acesso ao banco das palavras-chave. Só o que o cadastro usa; nada de exclusão. */
interface RepositorioDePalavrasChave extends Repository<PalavraChave, Long> {

    PalavraChave save(PalavraChave palavraChave);

    /** A comparação segue a collation da coluna: "farmacia" encontra "Farmácia". */
    boolean existsByTexto(String texto);
}
