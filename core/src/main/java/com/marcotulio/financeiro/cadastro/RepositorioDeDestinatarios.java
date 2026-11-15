package com.marcotulio.financeiro.cadastro;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Acesso ao banco dos destinatários.
 *
 * <p>Estende {@link Repository} (e não {@code JpaRepository}) para expor só os métodos abaixo:
 * o que não está declarado, como a exclusão, simplesmente não existe.
 */
interface RepositorioDeDestinatarios extends Repository<Destinatario, Long> {

    Destinatario save(Destinatario destinatario);

    /** A comparação segue a collation da coluna: {@code @MT} encontra {@code @mt}. */
    Optional<Destinatario> findByMarcacaoBot(String marcacaoBot);
}
