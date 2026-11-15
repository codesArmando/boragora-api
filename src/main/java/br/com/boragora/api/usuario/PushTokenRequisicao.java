package br.com.boragora.api.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Token do Expo do aparelho, no formato {@code ExponentPushToken[...]} (ou {@code ExpoPushToken[...]}). */
public record PushTokenRequisicao(
        @NotBlank(message = "Informe o token.")
        @Size(max = 255, message = "O token pode ter no máximo 255 caracteres.")
        @Pattern(regexp = "Expo(nent)?PushToken\\[.+]", message = "O token não está no formato do Expo.")
        String token) {

    public PushTokenRequisicao {
        token = token == null ? null : token.strip();
    }
}
