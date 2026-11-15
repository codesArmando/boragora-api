package br.com.boragora.api.viagem;

/**
 * {@code vaoNaIda} conta só a ida marcada: o cancelado tardio aparece na lista da ida, mas entra
 * apenas em {@code canceladosAposSaida}. {@code liberados} conta a volta marcada com status LIBERADO.
 */
public record ContadoresDoDia(long vaoNaIda, long voltam, long liberados, long canceladosAposSaida) {
}
