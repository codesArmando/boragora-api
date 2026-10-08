package br.com.boragora.api.turma;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TurmaAlunoRepository extends JpaRepository<TurmaAluno, Long> {

    boolean existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(Long alunoId, Long grupoId);
}
