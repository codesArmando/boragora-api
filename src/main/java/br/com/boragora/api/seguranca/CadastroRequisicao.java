package br.com.boragora.api.seguranca;

import br.com.boragora.api.usuario.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do cadastro público. Não há campo de papel nem de admin: o cadastro cria sempre um aluno. */
public record CadastroRequisicao(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255, message = "O e-mail pode ter no máximo 255 caracteres.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 6, max = 72, message = "A senha precisa ter de 6 a 72 caracteres.")
        String senha) {

    public CadastroRequisicao {
        // Normaliza antes da validação, para um espaço digitado nas pontas não invalidar o e-mail.
        email = Usuario.normalizarEmail(email);
    }
}
