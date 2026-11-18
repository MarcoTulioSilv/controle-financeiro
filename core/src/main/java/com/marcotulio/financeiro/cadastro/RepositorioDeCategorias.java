package com.marcotulio.financeiro.cadastro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Acesso ao banco das categorias.
 *
 * <p>Estende {@link Repository} (e não {@code JpaRepository}, que traria {@code delete} pronto)
 * para que excluir uma categoria seja impossível pelo código: RF02 manda desativar.
 */
interface RepositorioDeCategorias extends Repository<Categoria, Long> {

    Categoria save(Categoria categoria);

    Optional<Categoria> findById(Long id);

    /** A comparação segue a collation da coluna: "saude" encontra "Saúde". */
    Optional<Categoria> findByNomeAndTipo(String nome, TipoCategoria tipo);

    List<Categoria> findByTipoAndAtivaTrueOrderByNome(TipoCategoria tipo);
}
