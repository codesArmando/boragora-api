package br.com.boragora.api.turma;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.usuario.Usuario;
import jakarta.persistence.LockModeType;

public interface TurmaAlunoRepository extends JpaRepository<TurmaAluno, Long> {

    boolean existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(Long alunoId, Long grupoId);

    /** Vínculo do par turma e aluno, ativo ou não: existe no máximo um (restrição única). */
    Optional<TurmaAluno> findByTurmaIdAndAlunoId(Long turmaId, Long alunoId);

    @Query("select ta from TurmaAluno ta where ta.aluno.id = :alunoId and ta.turma.id = :turmaId and ta.ativo = true")
    Optional<TurmaAluno> findVinculoAtivo(Long alunoId, Long turmaId);

    /**
     * Mesmo que {@link #findVinculoAtivo}, travando só a linha do vínculo (a consulta não faz join),
     * para que duas alterações simultâneas do mesmo aluno na mesma turma sejam feitas uma depois da outra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ta from TurmaAluno ta where ta.aluno.id = :alunoId and ta.turma.id = :turmaId and ta.ativo = true")
    Optional<TurmaAluno> travarVinculoAtivo(Long alunoId, Long turmaId);

    @Query("""
            select ta from TurmaAluno ta join fetch ta.turma t join fetch t.grupo join fetch t.motorista
            where ta.aluno.id = :alunoId and ta.ativo = true
            """)
    List<TurmaAluno> findAtivosDoAlunoComTurma(Long alunoId);

    @Query("""
            select ta.turma from TurmaAluno ta
            where ta.aluno.id = :alunoId and ta.ativo = true and ta.turma.grupo.id = :grupoId
            """)
    List<Turma> findTurmasAtivasDoAlunoNoGrupo(Long alunoId, Long grupoId);

    @Query("""
            select ta from TurmaAluno ta join fetch ta.turma t join fetch t.grupo
            where ta.ativo = true and ta.aluno.id in :alunoIds
            """)
    List<TurmaAluno> findAtivosDosAlunos(Collection<Long> alunoIds);

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

    /**
     * Destinatários do lembrete (RF13): vínculos ativos nas turmas dos grupos, de alunos ativos com
     * token, que não marcaram ida nem volta no dia.
     */
    @Query("""
            select ta from TurmaAluno ta join fetch ta.aluno a join fetch ta.turma t
            where t.grupo.id in :grupoIds and ta.ativo = true and a.ativo = true and a.expoPushToken is not null
              and not exists (
                  select 1 from Confirmacao c
                  where c.turmaAluno = ta and c.viagem.data = :data and (c.incluiIda = true or c.incluiVolta = true))
            """)
    List<TurmaAluno> findSemPlanoNoDiaComToken(Collection<Long> grupoIds, LocalDate data);
}
