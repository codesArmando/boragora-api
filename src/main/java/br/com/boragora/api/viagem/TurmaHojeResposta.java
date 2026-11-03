package br.com.boragora.api.viagem;

import java.util.List;

import br.com.boragora.api.turma.GrupoDaTurma;
import br.com.boragora.api.turma.PessoaDaTurma;

/**
 * Dia de uma turma (telas C1, C2 e C5). Lista da ida: cancelados tardios primeiro, depois ordem
 * alfabética. Lista da volta: liberados primeiro, depois ordem alfabética.
 */
public record TurmaHojeResposta(
        Long id,
        String nome,
        GrupoDaTurma grupo,
        PessoaDaTurma motorista,
        ContadoresDoDia contadores,
        boolean semConfirmacoes,
        List<ItemDaIda> listaIda,
        List<ItemDaVolta> listaVolta) {
}
