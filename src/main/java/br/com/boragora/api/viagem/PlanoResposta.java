package br.com.boragora.api.viagem;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.boragora.api.turma.TurmaDoAluno;
import io.swagger.v3.oas.annotations.media.Schema;

/** Plano de hoje do aluno em uma turma (telas B1 a B7). Sem confirmação, tudo vem falso e o status {@code EM_AULA}. */
public record PlanoResposta(
        @JsonFormat(pattern = "yyyy-MM-dd") @Schema(type = "string", example = "2026-10-05") LocalDate data,
        TurmaDoAluno turma,
        boolean incluiIda,
        boolean incluiVolta,
        StatusVolta statusVolta,
        boolean cancelouAposSaida,
        boolean idaSaiu,
        boolean voltaSaiu,
        AcoesDoPlano acoes) {
}
