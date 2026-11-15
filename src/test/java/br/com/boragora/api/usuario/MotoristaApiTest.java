package br.com.boragora.api.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;

@TesteDeIntegracao
class MotoristaApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private UsuarioRepository usuarios;

    private Usuario chefe;
    private String admin;

    @BeforeEach
    void entrarComoAdmin() {
        chefe = dados.admin();
        admin = dados.bearer(chefe);
    }

    @Test
    void adminCadastraMotoristaQueConsegueEntrar() throws Exception {
        String email = DadosDeTeste.emailUnico("Carlos").toUpperCase();

        cadastrar(admin, corpo(" Carlos ", "  " + email + " ", "senha123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario gravado = usuarios.findByEmail(email.toLowerCase()).orElseThrow();
        assertThat(gravado.getPapel()).isEqualTo(Papel.MOTORISTA);
        assertThat(gravado.isAdmin()).isFalse();
        assertThat(gravado.getSenhaHash()).isNotEqualTo("senha123");

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"senha123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("MOTORISTA"));
    }

    @Test
    void cadastroIgnoraAdminNoCorpo() throws Exception {
        String email = DadosDeTeste.emailUnico("intruso");
        cadastrar(admin, """
                {"nome": "Intruso", "email": "%s", "senha": "senha123", "admin": true, "papel": "ALUNO"}
                """.formatted(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admin").value(false));
        Usuario gravado = usuarios.findByEmail(email).orElseThrow();
        assertThat(gravado.isAdmin()).isFalse();
        assertThat(gravado.getPapel()).isEqualTo(Papel.MOTORISTA);
    }

    @Test
    void emailRepetidoDevolve409() throws Exception {
        Usuario aluno = dados.aluno();
        cadastrar(admin, corpo("Carlos", aluno.getEmail(), "senha123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void camposInvalidosDevolvem400() throws Exception {
        cadastrar(admin, corpo(" ", "nao-e-email", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void listaMotoristasAtivosComOChefeEmOrdemAlfabetica() throws Exception {
        Usuario zeca = dados.motorista("Zeca Teste");
        Usuario ana = dados.motorista("Ana Teste");
        Usuario inativo = dados.motorista("Inativo Teste");
        inativo.setAtivo(false);
        usuarios.save(inativo);
        Usuario aluno = dados.aluno();

        String json = mvc.perform(get("/admin/motoristas").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(chefe.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(inativo.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(aluno.getId().intValue()))))
                .andExpect(jsonPath("$[?(@.id == %d)].admin".formatted(chefe.getId())).value(true))
                .andExpect(jsonPath("$[?(@.id == %d)].email".formatted(ana.getId())).value(ana.getEmail()))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids.indexOf(ana.getId().intValue())).isLessThan(ids.indexOf(zeca.getId().intValue()));
    }

    @Test
    void naoAdminRecebe403() throws Exception {
        for (String bearer : new String[] { dados.bearer(dados.aluno()), dados.bearer(dados.motorista()) }) {
            cadastrar(bearer, corpo("Carlos", DadosDeTeste.emailUnico("carlos"), "senha123"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mvc.perform(get("/admin/motoristas").header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden());
        }
    }

    private ResultActions cadastrar(String bearer, String corpo) throws Exception {
        return mvc.perform(post("/admin/motoristas").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(String nome, String email, String senha) {
        return """
                {"nome": "%s", "email": "%s", "senha": "%s"}
                """.formatted(nome, email, senha);
    }
}
