package br.com.boragora.api.turma;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpo da criação de turma (tela D4). {@code numeroVan} e {@code capacidade} são opcionais. */
public record TurmaCriacaoRequisicao(
        @NotNull(message = "Informe o grupo.")
        Long grupoId,

        @NotBlank(message = "Informe o nome da turma.")
        @Size(max = 60, message = "O nome da turma pode ter no máximo 60 caracteres.")
        String nome,

        @Positive(message = "O número da van precisa ser maior que zero.")
        Integer numeroVan,

        @Positive(message = "A capacidade precisa ser maior que zero.")
        Integer capacidade,

        @NotNull(message = "Informe o motorista.")
        Long motoristaId) {

    public TurmaCriacaoRequisicao {
        // Tira os espaços das pontas antes da validação: o limite de 60 vale para o nome gravado.
        nome = nome == null ? null : nome.strip();
    }
}
