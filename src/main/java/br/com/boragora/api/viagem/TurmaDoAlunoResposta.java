package br.com.boragora.api.viagem;

import br.com.boragora.api.turma.PessoaDaTurma;
import br.com.boragora.api.turma.TurmaDoAluno;

/** Item de {@code GET /aluno/turmas}: a turma, o motorista e o resumo do plano de hoje. */
public record TurmaDoAlunoResposta(TurmaDoAluno turma, PessoaDaTurma motorista, boolean incluiIda,
        boolean incluiVolta) {
}
