package br.com.boragora.api.seguranca;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class UsuarioAutenticadoToken extends AbstractAuthenticationToken {

    private final UsuarioAutenticado usuario;
    private final Jwt jwt;

    UsuarioAutenticadoToken(UsuarioAutenticado usuario, Jwt jwt) {
        super(usuario.autoridades());
        this.usuario = usuario;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public UsuarioAutenticado getPrincipal() {
        return usuario;
    }

    @Override
    public Jwt getCredentials() {
        return jwt;
    }
}
