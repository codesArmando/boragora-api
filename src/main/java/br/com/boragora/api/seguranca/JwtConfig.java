package br.com.boragora.api.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Emissão e validação do JWT (HS256). A validade é conferida com o mesmo {@link Clock} da aplicação. */
@Configuration
public class JwtConfig {

    static final String EMISSOR = "boragora-api";

    private final SecretKey chave;

    public JwtConfig(JwtPropriedades propriedades) {
        this.chave = new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder(Clock relogio) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validadeDoToken = new JwtTimestampValidator(Duration.ZERO);
        validadeDoToken.setClock(relogio);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadeDoToken, new JwtIssuerValidator(EMISSOR)));
        return decoder;
    }
}
