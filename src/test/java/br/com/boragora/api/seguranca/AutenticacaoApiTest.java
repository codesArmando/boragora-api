package br.com.boragora.api.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;

@TesteDeIntegracao
class AutenticacaoApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private JwtEncoder jwtEncoder;

    // --- cadastro e login ---

    @Test
    void cadastroSeguidoDeLoginDevolveTokenValidoEMeDevolveOUsuario() throws Exception {
        String email = DadosDeTeste.emailUnico("Ana");

        cadastrar("""
                {"nome": "  Ana Souza ", "email": "  %s ", "senha": "segredo1"}
                """.formatted(email.toUpperCase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.usuario.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.usuario.papel").value("ALUNO"))
                .andExpect(jsonPath("$.usuario.admin").value(false));

        String token = tokenDe(entrar(email.toUpperCase(), "segredo1").andExpect(status().isOk()));

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.papel").value("ALUNO"))
                .andExpect(jsonPath("$.admin").value(false));

        Usuario gravado = usuarios.findByEmail(email.toLowerCase()).orElseThrow();
        assertThat(gravado.getSenhaHash()).startsWith("$2").isNotEqualTo("segredo1");
    }

    @Test
    void cadastroComEmailRepetidoDevolve409() throws Exception {
        String email = DadosDeTeste.emailUnico("repetido");
        cadastrar(corpoCadastro(email)).andExpect(status().isCreated());

        cadastrar(corpoCadastro(email.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void cadastroIgnoraPapelEAdminEnviadosNoCorpo() throws Exception {
        String email = DadosDeTeste.emailUnico("espertinho");

        cadastrar("""
                {"nome": "Espertinho", "email": "%s", "senha": "segredo1",
                 "papel": "MOTORISTA", "role": "MOTORISTA", "admin": true}
                """.formatted(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.papel").value("ALUNO"))
                .andExpect(jsonPath("$.usuario.admin").value(false));

        Usuario gravado = usuarios.findByEmail(email).orElseThrow();
        assertThat(gravado.getPapel()).isEqualTo(Papel.ALUNO);
        assertThat(gravado.isAdmin()).isFalse();
    }

    @Test
    void cadastroComCamposInvalidosDevolve400ComOsCampos() throws Exception {
        cadastrar("""
                {"nome": " ", "email": "nao-e-email", "senha": "123"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void loginComSenhaErradaEmailInexistenteOuUsuarioInativoDevolveOMesmo401() throws Exception {
        Usuario aluno = dados.aluno();
        Usuario inativo = dados.aluno();
        inativo.setAtivo(false);
        usuarios.save(inativo);

        String senhaErrada = entrar(aluno.getEmail(), "senha-errada")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String emailInexistente = entrar(DadosDeTeste.emailUnico("ninguem"), DadosDeTeste.SENHA)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String usuarioInativo = entrar(inativo.getEmail(), DadosDeTeste.SENHA)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(senhaErrada, "$.codigo")).isEqualTo("CREDENCIAIS_INVALIDAS");
        assertThat(emailInexistente).isEqualTo(senhaErrada);
        assertThat(usuarioInativo).isEqualTo(senhaErrada);
    }

    // --- token ---

    @Test
    void rotaProtegidaSemTokenDevolve401() throws Exception {
        mvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenInvalidoDevolve401() throws Exception {
        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer nao.e.um-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        Usuario aluno = dados.aluno();
        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("boragora-api")
                .subject(aluno.getId().toString())
                .issuedAt(ontem.minus(1, ChronoUnit.DAYS))
                .expiresAt(ontem)
                .build();
        String expirado = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void usuarioDesativadoDepoisDoLoginPerdeOAcessoNaHora() throws Exception {
        Usuario aluno = dados.aluno();
        String bearer = dados.bearer(aluno);
        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());

        aluno.setAtivo(false);
        usuarios.save(aluno);

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    // --- regras por prefixo ---

    @Test
    void alunoEmRotaAdminRecebe403() throws Exception {
        mvc.perform(get("/admin/grupos").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void motoristaComumEmRotaAdminRecebe403() throws Exception {
        mvc.perform(get("/admin/grupos").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.motorista())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void alunoEmRotaDeMotoristaEMotoristaEmRotaDeAlunoRecebem403() throws Exception {
        mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.motorista())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassaPelasRegrasDeAdminEDeMotorista() throws Exception {
        // Caminhos sem rota: 404 (e não 403) mostra que a segurança deixou passar.
        String bearer = dados.bearer(dados.admin());
        mvc.perform(get("/admin/caminho-sem-rota").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/motorista/caminho-sem-rota").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void rotasPublicasNaoExigemToken() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    private ResultActions cadastrar(String corpo) throws Exception {
        return mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(email, senha)));
    }

    private static String corpoCadastro(String email) {
        return """
                {"nome": "Fulano", "email": "%s", "senha": "segredo1"}
                """.formatted(email);
    }

    private static String tokenDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.token");
    }
}
