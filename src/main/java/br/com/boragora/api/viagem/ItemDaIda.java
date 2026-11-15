package br.com.boragora.api.viagem;

/** {@code voce} marca o próprio aluno autenticado (selo "Você"). */
public record ItemDaIda(Long alunoId, String nome, SituacaoIda situacao, boolean voce) {
}
