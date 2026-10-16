package br.com.boragora.api.turma;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.boragora.api.grupo.Grupo;

public interface TurmaAlunoRepository extends JpaRepository<TurmaAluno, Long> {

    boolean existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(Long alunoId, Long grupoId);

    @Query("select ta.turma from TurmaAluno ta where ta.aluno.id = :alunoId and ta.ativo = true")
    List<Turma> findTurmasAtivasDoAluno(Long alunoId);

    @Query("select distinct ta.turma.grupo from TurmaAluno ta where ta.aluno.id = :alunoId and ta.ativo = true")
    List<Grupo> findGruposDoAluno(Long alunoId);
}
