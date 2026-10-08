package br.com.boragora.api.seguranca;

import br.com.boragora.api.usuario.UsuarioResposta;

public record TokenResposta(String token, UsuarioResposta usuario) {
}
