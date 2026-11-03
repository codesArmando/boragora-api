package br.com.boragora.api.viagem;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.turma.TurmaRepository;

/**
 * Cria a viagem do dia em uma transação própria. Se outro aluno criar a mesma viagem ao mesmo
 * tempo, a restrição única (id_turma, data) falha só aqui, sem marcar a transação de quem chamou
 * para rollback.
 */
@Component
public class CriadorDeViagem {

    private final ViagemRepository viagens;
    private final TurmaRepository turmas;

    public CriadorDeViagem(ViagemRepository viagens, TurmaRepository turmas) {
        this.viagens = viagens;
        this.turmas = turmas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void criar(Long turmaId, LocalDate data) {
        viagens.saveAndFlush(new Viagem(turmas.getReferenceById(turmaId), data));
    }
}
