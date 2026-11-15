package br.com.boragora.api.turma;

/** Item da lista cadastral de turmas (tela D3). {@code totalAlunos} conta só vínculos ativos. */
public record TurmaCadastroResposta(
        Long id,
        String nome,
        Integer numeroVan,
        Integer capacidade,
        GrupoDaTurma grupo,
        PessoaDaTurma motorista,
        long totalAlunos) {
}
