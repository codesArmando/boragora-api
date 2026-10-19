package br.com.boragora.api.seguranca;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propriedades {@code app.jwt.*}. A chave precisa ter ao menos 32 bytes para o HS256. */
@ConfigurationProperties("app.jwt")
public record JwtPropriedades(String segredo, int validadeHoras) {

    public JwtPropriedades {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.segredo (JWT_SEGREDO) precisa ter pelo menos 32 bytes.");
        }
        if (validadeHoras <= 0) {
            throw new IllegalStateException("app.jwt.validade-horas precisa ser maior que zero.");
        }
    }
}
