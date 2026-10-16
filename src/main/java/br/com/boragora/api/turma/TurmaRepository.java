package br.com.boragora.api.turma;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.boragora.api.grupo.Grupo;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    boolean existsByMotoristaIdAndGrupoId(Long motoristaId, Long grupoId);

    boolean existsByGrupoId(Long grupoId);

    @Query("select t.grupo.id from Turma t where t.id = :turmaId")
    Optional<Long> findGrupoIdById(Long turmaId);

    List<Turma> findByMotoristaId(Long motoristaId);

    @Query("select distinct t.grupo from Turma t where t.motorista.id = :motoristaId")
    List<Grupo> findGruposDoMotorista(Long motoristaId);

    @Query("""
            select new br.com.boragora.api.turma.TotalDeTurmas(t.grupo.id, count(t))
            from Turma t where t.grupo.id in :grupoIds group by t.grupo.id
            """)
    List<TotalDeTurmas> contarPorGrupo(Collection<Long> grupoIds);
}
