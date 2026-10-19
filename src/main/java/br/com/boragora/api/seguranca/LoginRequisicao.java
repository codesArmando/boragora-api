package br.com.boragora.api.seguranca;

import br.com.boragora.api.usuario.Usuario;
import jakarta.validation.constraints.NotBlank;

public record LoginRequisicao(
        @NotBlank(message = "Informe o e-mail.")
        String email,

        @NotBlank(message = "Informe a senha.")
        String senha) {

    public LoginRequisicao {
        email = Usuario.normalizarEmail(email);
    }
}
