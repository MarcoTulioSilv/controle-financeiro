package com.marcotulio.financeiro.cadastro;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Acesso ao banco dos usuários. Sem exclusão: só o que está declarado aqui existe. */
interface RepositorioDeUsuarios extends Repository<Usuario, Long> {

    Usuario save(Usuario usuario);

    /** A comparação segue a collation da coluna: {@code ANA} encontra {@code ana}. */
    Optional<Usuario> findByLogin(String login);
}
