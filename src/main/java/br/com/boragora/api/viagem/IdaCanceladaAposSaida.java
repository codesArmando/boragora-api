package br.com.boragora.api.viagem;

import java.time.LocalDate;

/**
 * Publicado quando o aluno cancela a ida depois da saída (RN05). Quem consome é o push
 * de cancelamento tardio da etapa 8, que avisa o motorista da turma.
 */
public record IdaCanceladaAposSaida(Long turmaId, Long alunoId, Long confirmacaoId, LocalDate data) {
}
