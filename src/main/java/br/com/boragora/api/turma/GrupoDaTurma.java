package br.com.boragora.api.turma;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.Periodo;
import io.swagger.v3.oas.annotations.media.Schema;

/** O grupo dentro das respostas de turma. */
public record GrupoDaTurma(
        Long id,
        String nome,
        Periodo periodo,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:10") LocalTime horarioSaidaIda,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "22:30") LocalTime horarioSaidaVolta) {

    public static GrupoDaTurma de(Grupo grupo) {
        return new GrupoDaTurma(grupo.getId(), grupo.getNome(), grupo.getPeriodo(),
                grupo.getHorarioSaidaIda(), grupo.getHorarioSaidaVolta());
    }
}
