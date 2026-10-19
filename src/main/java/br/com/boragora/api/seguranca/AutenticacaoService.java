package br.com.boragora.api.seguranca;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.usuario.UsuarioResposta;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    /** Usado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos. */
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.hashFicticio = senhas.encode("senha-ficticia-para-comparacao");
    }

    /** Cadastro público (RF07): cria sempre um aluno comum, sem vínculo com turma. */
    @Transactional
    public TokenResposta cadastrar(CadastroRequisicao requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        if (usuarios.existsByEmail(email)) {
            throw emailJaCadastrado();
        }
        Usuario usuario = new Usuario(requisicao.nome().trim(), email, senhas.encode(requisicao.senha()), Papel.ALUNO);
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros simultâneos com o mesmo e-mail: a restrição única decide.
            throw emailJaCadastrado();
        }
        return resposta(usuario);
    }

    /** E-mail inexistente, senha errada e usuário inativo devolvem o mesmo erro. */
    @Transactional(readOnly = true)
    public TokenResposta entrar(LoginRequisicao requisicao) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(requisicao.email())).orElse(null);
        boolean senhaConfere = senhas.matches(requisicao.senha(), usuario != null ? usuario.getSenhaHash() : hashFicticio);
        if (usuario == null || !senhaConfere || !usuario.isAtivo()) {
            throw new RegraDeNegocioException("CREDENCIAIS_INVALIDAS", HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        return resposta(usuario);
    }

    private TokenResposta resposta(Usuario usuario) {
        return new TokenResposta(tokens.emitir(usuario), UsuarioResposta.de(usuario));
    }

    private static RegraDeNegocioException emailJaCadastrado() {
        return new RegraDeNegocioException("EMAIL_JA_CADASTRADO", HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
    }
}
