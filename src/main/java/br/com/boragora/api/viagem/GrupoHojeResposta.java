package br.com.boragora.api.viagem;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.boragora.api.grupo.GrupoResposta.MinhaTurma;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.turma.PessoaDaTurma;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dia de todas as turmas de um grupo (telas B8, B8b e C3). {@code minhaTurma} do grupo é a turma de
 * quem chama, ou {@code null}; cada turma traz {@code minhaTurma} como verdadeiro ou falso.
 */
public record GrupoHojeResposta(
        Long id,
        String nome,
        Periodo periodo,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:10") LocalTime horarioSaidaIda,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "22:30") LocalTime horarioSaidaVolta,
        MinhaTurma minhaTurma,
        List<TurmaDoGrupoHoje> turmas) {

    public record TurmaDoGrupoHoje(
            Long id,
            String nome,
            PessoaDaTurma motorista,
            boolean minhaTurma,
            ContadoresDoDia contadores,
            boolean semConfirmacoes,
            List<ItemDaIda> listaIda,
            List<ItemDaVolta> listaVolta) {
    }
}
