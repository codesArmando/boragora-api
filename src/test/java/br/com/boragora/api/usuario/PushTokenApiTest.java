package br.com.boragora.api.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;

@TesteDeIntegracao
class PushTokenApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private UsuarioRepository usuarios;

    @Test
    void registrarERemoverOToken() throws Exception {
        Usuario aluno = dados.aluno();
        String token = token();

        registrar(dados.bearer(aluno), "{\"token\": \" %s \"}".formatted(token)).andExpect(status().isNoContent());
        assertThat(tokenDe(aluno)).isEqualTo(token);

        mvc.perform(delete("/me/push-token").header(HttpHeaders.AUTHORIZATION, dados.bearer(aluno)))
                .andExpect(status().isNoContent());
        assertThat(tokenDe(aluno)).isNull();
        // Remover de novo não é erro.
        mvc.perform(delete("/me/push-token").header(HttpHeaders.AUTHORIZATION, dados.bearer(aluno)))
                .andExpect(status().isNoContent());
    }

    @Test
    void motoristaTambemRegistraToken() throws Exception {
        Usuario motorista = dados.motorista();
        String token = "ExpoPushToken[" + UUID.randomUUID() + "]";
        registrar(dados.bearer(motorista), corpo(token)).andExpect(status().isNoContent());
        assertThat(tokenDe(motorista)).isEqualTo(token);
    }

    @Test
    void oMesmoAparelhoEmOutraContaSaiDaContaAnterior() throws Exception {
        Usuario anterior = dados.aluno();
        Usuario atual = dados.aluno();
        String token = token();
        registrar(dados.bearer(anterior), corpo(token)).andExpect(status().isNoContent());

        registrar(dados.bearer(atual), corpo(token)).andExpect(status().isNoContent());

        assertThat(tokenDe(anterior)).isNull();
        assertThat(tokenDe(atual)).isEqualTo(token);
    }

    @Test
    void tokenAusenteOuForaDoFormatoDoExpoDevolve400() throws Exception {
        String bearer = dados.bearer(dados.aluno());
        for (String corpo : new String[] { "{}", "{\"token\": \"\"}", "{\"token\": \"abc\"}",
                "{\"token\": \"ExponentPushToken[]\"}" }) {
            registrar(bearer, corpo)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.token").exists());
        }
    }

    @Test
    void semTokenDeAcessoDevolve401() throws Exception {
        mvc.perform(put("/me/push-token").contentType(MediaType.APPLICATION_JSON).content(corpo(token())))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/me/push-token")).andExpect(status().isUnauthorized());
    }

    private ResultActions registrar(String bearer, String corpo) throws Exception {
        return mvc.perform(put("/me/push-token").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String tokenDe(Usuario usuario) {
        return usuarios.findById(usuario.getId()).orElseThrow().getExpoPushToken();
    }

    private static String token() {
        return "ExponentPushToken[" + UUID.randomUUID() + "]";
    }

    private static String corpo(String token) {
        return "{\"token\": \"%s\"}".formatted(token);
    }
}
