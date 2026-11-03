package br.com.boragora.api.turma;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Corpo do vínculo de alunos (tela D6). Ids repetidos contam uma vez só. */
public record VinculoRequisicao(
        @NotEmpty(message = "Informe ao menos um aluno.")
        List<@NotNull(message = "O id do aluno não pode ser nulo.") Long> alunoIds) {
}
