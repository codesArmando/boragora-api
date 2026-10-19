package br.com.boragora.api.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

@TesteDeIntegracao
class AdminInicialTest {

    // Valores do perfil test (application.yml).
    private static final String EMAIL = "admin@teste.com";
    private static final String SENHA = "senha-teste";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private AdminInicial adminInicial;

    @Test
    void adminInicialECriadoNaSubidaUmaUnicaVez() throws Exception {
        assertThat(contarAdminInicial()).isEqualTo(1);
        Usuario admin = usuarios.findByEmail(EMAIL).orElseThrow();
        assertThat(admin.getPapel()).isEqualTo(Papel.MOTORISTA);
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.isAtivo()).isTrue();

        // Uma nova subida não cria outro.
        adminInicial.run(new DefaultApplicationArguments());
        adminInicial.run(new DefaultApplicationArguments());
        assertThat(contarAdminInicial()).isEqualTo(1);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("MOTORISTA"))
                .andExpect(jsonPath("$.usuario.admin").value(true));
    }

    private long contarAdminInicial() {
        return usuarios.findAll().stream().filter(u -> u.getEmail().equals(EMAIL)).count();
    }
}
