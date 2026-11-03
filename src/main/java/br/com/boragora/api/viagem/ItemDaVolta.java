package br.com.boragora.api.viagem;

/** {@code voce} marca o próprio aluno autenticado (selo "Você"). */
public record ItemDaVolta(Long alunoId, String nome, StatusVolta statusVolta, boolean voce) {
}
