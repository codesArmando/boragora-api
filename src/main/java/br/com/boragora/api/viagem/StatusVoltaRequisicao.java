package br.com.boragora.api.viagem;

import jakarta.validation.constraints.NotNull;

public record StatusVoltaRequisicao(@NotNull(message = "Informe o status da volta.") StatusVolta status) {
}
