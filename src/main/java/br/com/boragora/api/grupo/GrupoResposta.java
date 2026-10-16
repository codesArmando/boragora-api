package br.com.boragora.api.grupo;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Grupo como aparece nas telas D1, D2, B8a e C3a. {@code minhaTurma} é a turma de quem
 * chama naquele grupo (como aluno com vínculo ativo ou como motorista), ou {@code null}.
 */
public record GrupoResposta(
        Long id,
        String nome,
        Periodo periodo,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:10") LocalTime horarioSaidaIda,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "22:30") LocalTime horarioSaidaVolta,
        long totalTurmas,
        MinhaTurma minhaTurma) {

    public record MinhaTurma(Long id, String nome) {
    }
}
