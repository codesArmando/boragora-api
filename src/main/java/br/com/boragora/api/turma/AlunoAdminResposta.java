package br.com.boragora.api.turma;

import java.util.List;

/**
 * Aluno nas telas D6 e D10. {@code turmas} são os vínculos ativos ({@code []} quando não há).
 * {@code situacao} e {@code turmaEmConflito} só vêm quando a listagem é feita para uma turma;
 * {@code turmaEmConflito} só quando a situação é {@code CONFLITO_NO_GRUPO}.
 */
public record AlunoAdminResposta(
        Long id,
        String nome,
        String email,
        List<TurmaDoAluno> turmas,
        SituacaoAluno situacao,
        TurmaDoAluno turmaEmConflito) {
}
