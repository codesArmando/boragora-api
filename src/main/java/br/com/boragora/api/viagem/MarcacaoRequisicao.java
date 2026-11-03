package br.com.boragora.api.viagem;

import jakarta.validation.constraints.NotNull;

/** Corpo dos {@code PUT} da ida e da volta: marca ({@code true}) ou desmarca ({@code false}). */
public record MarcacaoRequisicao(@NotNull(message = "Informe se a opção deve ser incluída.") Boolean incluir) {
}
