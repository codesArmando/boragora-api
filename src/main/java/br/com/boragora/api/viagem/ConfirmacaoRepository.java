package br.com.boragora.api.viagem;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConfirmacaoRepository extends JpaRepository<Confirmacao, Long> {

    @Query("select c from Confirmacao c where c.turmaAluno.id = :turmaAlunoId and c.viagem.data = :data")
    Optional<Confirmacao> findDoDia(Long turmaAlunoId, LocalDate data);

    @Query("select c from Confirmacao c where c.turmaAluno.id in :turmaAlunoIds and c.viagem.data = :data")
    List<Confirmacao> findDoDia(Collection<Long> turmaAlunoIds, LocalDate data);

    /**
     * Tudo o que as listas do dia mostram, de várias turmas, em uma consulta: só vínculos ativos e só
     * quem declarou algo (ida, volta ou cancelamento tardio). Quem não declarou não aparece (RN04).
     */
    @Query("""
            select c from Confirmacao c
            join fetch c.viagem v
            join fetch c.turmaAluno ta
            join fetch ta.aluno
            where v.turma.id in :turmaIds and v.data = :data and ta.ativo = true
              and (c.incluiIda = true or c.incluiVolta = true or c.cancelouAposSaida = true)
            """)
    List<Confirmacao> findDasListasDoDia(Collection<Long> turmaIds, LocalDate data);
}
