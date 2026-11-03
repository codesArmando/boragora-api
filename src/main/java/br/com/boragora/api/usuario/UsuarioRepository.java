package br.com.boragora.api.usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAdminTrue();

    List<Usuario> findByPapelAndAtivoTrueOrderByNomePessoaAscIdAsc(Papel papel);

    /** {@code busca} nula traz todos. A collation do banco já ignora maiúsculas e acentos. */
    @Query("""
            select u from Usuario u
            where u.papel = :papel and u.ativo = true
              and (:busca is null
                   or u.nomePessoa like concat('%', :busca, '%')
                   or u.email like concat('%', :busca, '%'))
            order by u.nomePessoa, u.id
            """)
    List<Usuario> buscarAtivos(Papel papel, String busca);

    /** Trava as linhas em ordem de id, para que travas simultâneas não se cruzem. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id in :ids order by u.id")
    List<Usuario> travarPorIds(Collection<Long> ids);
}
