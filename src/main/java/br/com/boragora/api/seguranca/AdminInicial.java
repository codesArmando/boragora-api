package br.com.boragora.api.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

/**
 * Cria o motorista-chefe inicial na subida da aplicação, se ainda não existir nenhum admin.
 * É a única forma de o primeiro admin existir, já que o cadastro público só cria alunos.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final AdminPropriedades propriedades;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder senhas, AdminPropriedades propriedades) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.propriedades = propriedades;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByAdminTrue()) {
            return;
        }
        if (!propriedades.preenchidas()) {
            throw new IllegalStateException(
                    "Não existe motorista-chefe. Defina ADMIN_NOME, ADMIN_EMAIL e ADMIN_SENHA para criá-lo.");
        }
        String email = Usuario.normalizarEmail(propriedades.email());
        if (usuarios.existsByEmail(email)) {
            throw new IllegalStateException("ADMIN_EMAIL já pertence a um usuário que não é admin: " + email);
        }
        Usuario admin = new Usuario(propriedades.nome().trim(), email, senhas.encode(propriedades.senha()), Papel.MOTORISTA);
        admin.setAdmin(true);
        usuarios.save(admin);
        log.info("Motorista-chefe inicial criado: {}", email);
    }
}
