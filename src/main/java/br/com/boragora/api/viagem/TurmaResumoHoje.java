package br.com.boragora.api.viagem;

import br.com.boragora.api.turma.GrupoDaTurma;
import br.com.boragora.api.turma.PessoaDaTurma;

/** Item de {@code GET /motorista/turmas} (telas C0 e C6): a turma e os contadores de hoje, sem as listas. */
public record TurmaResumoHoje(
        Long id,
        String nome,
        GrupoDaTurma grupo,
        PessoaDaTurma motorista,
        ContadoresDoDia contadores,
        boolean semConfirmacoes) {
}
