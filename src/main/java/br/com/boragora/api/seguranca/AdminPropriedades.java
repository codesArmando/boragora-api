package br.com.boragora.api.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dados do motorista-chefe inicial ({@code app.admin.*}), lidos de ADMIN_NOME, ADMIN_EMAIL e ADMIN_SENHA. */
@ConfigurationProperties("app.admin")
public record AdminPropriedades(String nome, String email, String senha) {

    boolean preenchidas() {
        return nome != null && !nome.isBlank()
                && email != null && !email.isBlank()
                && senha != null && !senha.isBlank();
    }
}
