package br.com.boragora.api.turma;

import br.com.boragora.api.usuario.Usuario;

/** Motorista ou aluno dentro das respostas de turma. */
public record PessoaDaTurma(Long id, String nome) {

    public static PessoaDaTurma de(Usuario usuario) {
        return new PessoaDaTurma(usuario.getId(), usuario.getNomePessoa());
    }
}
