package br.com.boragora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * Ponta a ponta, só pela API e com os tokens de login: o admin monta grupo, motorista e turma;
 * o aluno se cadastra e é vinculado; declara o plano; o motorista vê a lista; o aluno cancela
 * depois da saída; a lista mostra o cancelado e o motorista recebe o push.
 */
@TesteDeIntegracao
class FluxoCompletoApiTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 5);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RelogioDeTeste relogio;
    @Autowired
    private PushGatewayFalso gateway;

    @AfterEach
    void limpar() {
        relogio.voltarAoReal();
        gateway.limpar();
    }

    @Test
    void doCadastroDoGrupoAteAListaComCancelamentoTardio() throws Exception {
        as("16:00");
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String admin = entrar("admin@teste.com", "senha-teste");

        Number grupoId = id(chamar(post("/admin/grupos"), admin, """
                {"nome": "Noite", "periodo": "NOITE", "horarioSaidaIda": "18:10", "horarioSaidaVolta": "22:30"}
                """).andExpect(status().isCreated()));
        String emailMotorista = "carlos-" + sufixo + "@teste.com";
        Number motoristaId = id(chamar(post("/admin/motoristas"), admin, """
                {"nome": "Carlos Menezes", "email": "%s", "senha": "senha123"}
                """.formatted(emailMotorista)).andExpect(status().isCreated()));
        Number turmaId = id(chamar(post("/admin/turmas"), admin, """
                {"grupoId": %s, "nome": "Van 2", "numeroVan": 2, "capacidade": 15, "motoristaId": %s}
                """.formatted(grupoId, motoristaId)).andExpect(status().isCreated()));

        String emailAluno = "joao-" + sufixo + "@teste.com";
        String cadastro = mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome": "João Gabriel", "email": "%s", "senha": "senha123"}
                """.formatted(emailAluno)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String aluno = "Bearer " + JsonPath.read(cadastro, "$.token");
        Number alunoId = JsonPath.read(cadastro, "$.usuario.id");

        // Antes do vínculo, o aluno não tem turma (tela A3).
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(jsonPath("$.length()").value(0));
        chamar(post("/admin/turmas/{id}/alunos", turmaId), admin, "{\"alunoIds\": [%s]}".formatted(alunoId))
                .andExpect(status().isOk());

        String motorista = entrar(emailMotorista, "senha123");
        chamar(put("/me/push-token"), motorista, "{\"token\": \"ExponentPushToken[%s]\"}".formatted(sufixo))
                .andExpect(status().isNoContent());

        as("17:00");
        chamar(put("/aluno/turmas/{id}/plano/ida", turmaId), aluno, "{\"incluir\": true}")
                .andExpect(status().isOk());
        chamar(put("/aluno/turmas/{id}/plano/volta", turmaId), aluno, "{\"incluir\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoes.cancelamentoIdaSeraTardio").value(false));

        mvc.perform(get("/turmas/{id}/hoje", turmaId).header(HttpHeaders.AUTHORIZATION, motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(1))
                .andExpect(jsonPath("$.listaIda[0].nome").value("João Gabriel"))
                .andExpect(jsonPath("$.listaIda[0].situacao").value("CONFIRMADO"));

        as("18:30");
        mvc.perform(get("/aluno/turmas/{id}/plano", turmaId).header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(jsonPath("$.acoes.cancelamentoIdaSeraTardio").value(true));
        chamar(put("/aluno/turmas/{id}/plano/ida", turmaId), aluno, "{\"incluir\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelouAposSaida").value(true));

        mvc.perform(get("/turmas/{id}/hoje", turmaId).header(HttpHeaders.AUTHORIZATION, motorista))
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(0))
                .andExpect(jsonPath("$.contadores.canceladosAposSaida").value(1))
                .andExpect(jsonPath("$.listaIda[0].alunoId").value(alunoId))
                .andExpect(jsonPath("$.listaIda[0].situacao").value("CANCELADO_APOS_SAIDA"));
        assertThat(gateway.enviadasPara("ExponentPushToken[%s]".formatted(sufixo)))
                .singleElement()
                .satisfies(push -> assertThat(push.titulo()).isEqualTo("João Gabriel cancelou a ida para a faculdade!"));

        // O aluno vê o próprio grupo com o selo "Você".
        mvc.perform(get("/grupos/{id}/hoje", grupoId).header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(jsonPath("$.minhaTurma.id").value(turmaId))
                .andExpect(jsonPath("$.turmas[0].listaIda[0].voce").value(true));
    }

    private void as(String horario) {
        relogio.fixar(LocalDateTime.of(DIA, LocalTime.parse(horario)));
    }

    private String entrar(String email, String senha) throws Exception {
        String json = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private ResultActions chamar(MockHttpServletRequestBuilder requisicao, String bearer, String corpo)
            throws Exception {
        return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static Number id(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
