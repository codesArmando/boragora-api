package br.com.boragora.api.turma;

import java.util.List;

/**
 * Turma com seus alunos de vínculo ativo, em ordem alfabética (telas C4, D5 e D5a).
 * {@code numeroVan} e {@code capacidade} podem vir nulos.
 */
public record TurmaResposta(
        Long id,
        String nome,
        Integer numeroVan,
        Integer capacidade,
        GrupoDaTurma grupo,
        PessoaDaTurma motorista,
        List<PessoaDaTurma> alunos) {
}
