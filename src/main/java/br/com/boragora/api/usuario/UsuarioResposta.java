package br.com.boragora.api.usuario;

import br.com.boragora.api.seguranca.UsuarioAutenticado;

public record UsuarioResposta(Long id, String nome, String email, Papel papel, boolean admin) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getNomePessoa(), usuario.getEmail(),
                usuario.getPapel(), usuario.isAdmin());
    }

    public static UsuarioResposta de(UsuarioAutenticado usuario) {
        return new UsuarioResposta(usuario.id(), usuario.nome(), usuario.email(), usuario.papel(), usuario.admin());
    }
}
