package br.com.boragora.api.turma;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    boolean existsByMotoristaIdAndGrupoId(Long motoristaId, Long grupoId);

    @Query("select t.grupo.id from Turma t where t.id = :turmaId")
    Optional<Long> findGrupoIdById(Long turmaId);
}
