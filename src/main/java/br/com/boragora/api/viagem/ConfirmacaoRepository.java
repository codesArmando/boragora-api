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
}
