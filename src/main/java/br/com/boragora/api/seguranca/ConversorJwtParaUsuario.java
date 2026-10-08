package br.com.boragora.api.seguranca;

import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

/**
 * Recarrega o usuário do token a cada requisição, para que {@code ativo} e {@code admin}
 * valham na hora. Usuário apagado ou inativo é tratado como token inválido (401).
 */
@Component
class ConversorJwtParaUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarios;

    ConversorJwtParaUsuario(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = idDoToken(jwt)
                .flatMap(usuarios::findById)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário inexistente ou inativo."));
        return new UsuarioAutenticadoToken(UsuarioAutenticado.de(usuario), jwt);
    }

    private static Optional<Long> idDoToken(Jwt jwt) {
        try {
            return Optional.of(Long.valueOf(jwt.getSubject()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
