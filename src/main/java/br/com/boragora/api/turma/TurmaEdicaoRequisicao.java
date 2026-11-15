package br.com.boragora.api.turma;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo da edição de turma (telas D5 e D5a). Substitui todos os dados cadastrais:
 * {@code numeroVan} ou {@code capacidade} ausentes ficam nulos. O grupo não muda.
 */
public record TurmaEdicaoRequisicao(
        @NotBlank(message = "Informe o nome da turma.")
        @Size(max = 60, message = "O nome da turma pode ter no máximo 60 caracteres.")
        String nome,

        @Positive(message = "O número da van precisa ser maior que zero.")
        Integer numeroVan,

        @Positive(message = "A capacidade precisa ser maior que zero.")
        Integer capacidade,

        @NotNull(message = "Informe o motorista.")
        Long motoristaId) {

    public TurmaEdicaoRequisicao {
        nome = nome == null ? null : nome.strip();
    }
}
