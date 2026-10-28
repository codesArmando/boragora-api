package br.com.boragora.api.turma;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.usuario.Usuario;

public interface TurmaAlunoRepository extends JpaRepository<TurmaAluno, Long> {

    boolean existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(Long alunoId, Long grupoId);

    @Query("select ta.turma from TurmaAluno ta where ta.aluno.id = :alunoId and ta.ativo = true")
    List<Turma> findTurmasAtivasDoAluno(Long alunoId);

    @Query("select distinct ta.turma.grupo from TurmaAluno ta where ta.aluno.id = :alunoId and ta.ativo = true")
    List<Grupo> findGruposDoAluno(Long alunoId);

    @Query("""
            select ta.aluno from TurmaAluno ta
            where ta.turma.id = :turmaId and ta.ativo = true
            order by ta.aluno.nomePessoa, ta.aluno.id
            """)
    List<Usuario> findAlunosAtivosDaTurma(Long turmaId);

    @Query("""
            select new br.com.boragora.api.turma.TotalDeAlunos(ta.turma.id, count(ta))
            from TurmaAluno ta where ta.ativo = true group by ta.turma.id
            """)
    List<TotalDeAlunos> contarAtivosPorTurma();
}
