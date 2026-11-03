package br.com.boragora.api.turma;

/** Situação do aluno em relação à turma em que se quer vinculá-lo (tela D6). */
public enum SituacaoAluno {
    /** Sem turma no grupo: pode ser vinculado. */
    DISPONIVEL,
    /** Já está em outra turma do mesmo grupo (RN08): a tela desabilita a seleção. */
    CONFLITO_NO_GRUPO
}
