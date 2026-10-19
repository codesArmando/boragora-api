package br.com.boragora.api.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.boragora.api.usuario.Usuario;

/** Emite o token de acesso. O token carrega só o id do usuário; o resto é lido do banco a cada requisição. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Clock relogio;
    private final Duration validade;

    public TokenService(JwtEncoder encoder, Clock relogio, JwtPropriedades propriedades) {
        this.encoder = encoder;
        this.relogio = relogio;
        this.validade = Duration.ofHours(propriedades.validadeHoras());
    }

    public String emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
