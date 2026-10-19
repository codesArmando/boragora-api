package br.com.boragora.api.grupo;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de criação e edição de grupo. A ordem dos horários é conferida no service. */
public record GrupoRequisicao(
        @NotBlank(message = "Informe o nome do grupo.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "Informe o período.")
        Periodo periodo,

        @NotNull(message = "Informe o horário de saída da ida.")
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", pattern = "HH:mm", example = "18:10")
        LocalTime horarioSaidaIda,

        @NotNull(message = "Informe o horário de saída da volta.")
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", pattern = "HH:mm", example = "22:30")
        LocalTime horarioSaidaVolta) {
}
