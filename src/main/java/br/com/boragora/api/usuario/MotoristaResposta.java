package br.com.boragora.api.usuario;

/** Motorista como aparece na tela D9 e nos seletores das telas D4 e D5. {@code admin} marca o motorista-chefe. */
public record MotoristaResposta(Long id, String nome, String email, boolean admin) {

    public static MotoristaResposta de(Usuario usuario) {
        return new MotoristaResposta(usuario.getId(), usuario.getNomePessoa(), usuario.getEmail(), usuario.isAdmin());
    }
}
