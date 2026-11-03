package br.com.boragora.api.viagem;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {

    Optional<Viagem> findByTurmaIdAndData(Long turmaId, LocalDate data);

    /** Leitura com trava: enxerga a viagem que outra transação acabou de gravar. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select v from Viagem v where v.turma.id = :turmaId and v.data = :data")
    Optional<Viagem> lerAtualPorTurmaEData(Long turmaId, LocalDate data);
}
