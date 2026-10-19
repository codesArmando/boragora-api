package br.com.boragora.api.seguranca;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;

/**
 * Quem está chamando a API, montado a partir do banco a cada requisição.
 * Nos controllers: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
public record UsuarioAutenticado(Long id, String nome, String email, Papel papel, boolean admin) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNomePessoa(), usuario.getEmail(),
                usuario.getPapel(), usuario.isAdmin());
    }

    /** {@code ROLE_ALUNO} ou {@code ROLE_MOTORISTA}, mais {@code ROLE_ADMIN} para o motorista-chefe. */
    public List<GrantedAuthority> autoridades() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + papel.name()));
        if (ehAdmin()) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return autoridades;
    }

    /** Só motorista pode ser motorista-chefe. */
    public boolean ehAdmin() {
        return admin && papel == Papel.MOTORISTA;
    }
}
