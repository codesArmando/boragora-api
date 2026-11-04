package br.com.boragora.api.usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;

@Service
public class PushTokenService {

    private final UsuarioRepository usuarios;

    public PushTokenService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** O app envia o token depois do login. Se o aparelho era de outra conta, ela deixa de receber push nele. */
    @Transactional
    public void registrar(Long usuarioId, String token) {
        usuarios.liberarTokenDeOutrasContas(token, usuarioId);
        buscar(usuarioId).setExpoPushToken(token);
    }

    /** O app remove o token ao sair da conta. */
    @Transactional
    public void remover(Long usuarioId) {
        buscar(usuarioId).setExpoPushToken(null);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Usuário não encontrado."));
    }
}
