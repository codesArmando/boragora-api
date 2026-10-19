package br.com.boragora.api;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.boragora.api.seguranca.TokenService;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

/** Cria usuários direto no banco e emite tokens para eles. */
@Component
public class DadosDeTeste {

    public static final String SENHA = "senha123";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;

    public DadosDeTeste(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    public static String emailUnico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    public Usuario aluno() {
        return usuarios.save(new Usuario("Aluno", emailUnico("aluno"), senhas.encode(SENHA), Papel.ALUNO));
    }

    public Usuario motorista() {
        return usuarios.save(new Usuario("Motorista", emailUnico("motorista"), senhas.encode(SENHA), Papel.MOTORISTA));
    }

    public Usuario admin() {
        Usuario admin = new Usuario("Chefe", emailUnico("chefe"), senhas.encode(SENHA), Papel.MOTORISTA);
        admin.setAdmin(true);
        return usuarios.save(admin);
    }

    public String bearer(Usuario usuario) {
        return "Bearer " + tokens.emitir(usuario);
    }
}
