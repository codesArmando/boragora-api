package br.com.boragora.api.grupo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;

@TesteDeIntegracao
class GrupoApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;

    private String admin;

    @BeforeEach
    void entrarComoAdmin() {
        admin = dados.bearer(dados.admin());
    }

    // --- CRUD do admin ---

    @Test
    void adminCriaConsultaEditaEExcluiGrupo() throws Exception {
        Long id = idDe(criar(admin, corpo("Noite", "NOITE", "18:10", "22:30"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Noite"))
                .andExpect(jsonPath("$.periodo").value("NOITE"))
                .andExpect(jsonPath("$.horarioSaidaIda").value("18:10"))
                .andExpect(jsonPath("$.horarioSaidaVolta").value("22:30"))
                .andExpect(jsonPath("$.totalTurmas").value(0)));

        mvc.perform(get("/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Noite"));

        mvc.perform(put("/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("  Noite B ", "TARDE", "17:45", "21:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Noite B"))
                .andExpect(jsonPath("$.periodo").value("TARDE"))
                .andExpect(jsonPath("$.horarioSaidaIda").value("17:45"))
                .andExpect(jsonPath("$.horarioSaidaVolta").value("21:00"));

        mvc.perform(get("/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.nome").value("Noite B"))
                .andExpect(jsonPath("$.horarioSaidaIda").value("17:45"));

        mvc.perform(delete("/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        mvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[*].id", not(hasItem(id.intValue()))));
    }

    @Test
    void naoAdminRecebe403NasRotasAdminGrupos() throws Exception {
        Grupo grupo = grupo();
        for (String bearer : new String[] { dados.bearer(dados.aluno()), dados.bearer(dados.motorista()) }) {
            criar(bearer, corpo("Noite", "NOITE", "18:10", "22:30"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mvc.perform(put("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON).content(corpo("X", "NOITE", "18:10", "22:30")))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden());
        }
        assertThat(grupos.findById(grupo.getId()).orElseThrow().getNome()).isEqualTo("Grupo");
    }

    // --- validação ---

    @Test
    void horarioDeIdaIgualOuPosteriorAoDeVoltaDevolve400() throws Exception {
        criar(admin, corpo("Noite", "NOITE", "22:30", "22:30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.horarioSaidaVolta").exists());
        criar(admin, corpo("Noite", "NOITE", "23:00", "22:30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));

        Grupo grupo = grupo();
        mvc.perform(put("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("Noite", "NOITE", "22:30", "18:10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.horarioSaidaVolta").exists());
        assertThat(grupos.findById(grupo.getId()).orElseThrow().getHorarioSaidaIda()).isEqualTo(LocalTime.of(18, 10));
    }

    @Test
    void camposObrigatoriosAusentesDevolvem400ComOsCampos() throws Exception {
        criar(admin, "{\"nome\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.periodo").exists())
                .andExpect(jsonPath("$.campos.horarioSaidaIda").exists())
                .andExpect(jsonPath("$.campos.horarioSaidaVolta").exists());
    }

    @Test
    void formatoInvalidoDeHorarioOuPeriodoDevolve400() throws Exception {
        criar(admin, corpo("Noite", "NOITE", "18h10", "22:30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        criar(admin, corpo("Noite", "MADRUGADA", "18:10", "22:30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    // --- exclusão ---

    @Test
    void excluirGrupoComTurmaDevolve409EMantemOGrupo() throws Exception {
        Grupo grupo = grupo();
        turmas.save(new Turma(grupo, dados.motorista(), "Van 1", 1, 15));

        mvc.perform(delete("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("GRUPO_COM_TURMAS"));
        assertThat(grupos.existsById(grupo.getId())).isTrue();
    }

    @Test
    void editarOuExcluirGrupoInexistenteDevolve404() throws Exception {
        mvc.perform(put("/admin/grupos/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("Noite", "NOITE", "18:10", "22:30")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/admin/grupos/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    // --- listagem e visibilidade ---

    @Test
    void adminVeTodosOsGrupos() throws Exception {
        Grupo a = grupo();
        Grupo b = grupo();

        String json = mvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<Integer>read(json, "$.length()")).isEqualTo((int) grupos.count());
        assertThat(JsonPath.<java.util.List<Integer>>read(json, "$[*].id"))
                .contains(a.getId().intValue(), b.getId().intValue());
    }

    @Test
    void alunoSemVinculoRecebeListaVaziaE403NoDetalhe() throws Exception {
        Grupo grupo = grupo();
        String aluno = dados.bearer(dados.aluno());

        mvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void alunoVeSoOsGruposComVinculoAtivoComSuaTurmaETotalDeTurmas() throws Exception {
        Grupo meu = grupo();
        Grupo removido = grupo();
        grupo(); // grupo sem relação com o aluno
        Turma minhaVan = turmas.save(new Turma(meu, dados.motorista(), "Van da Zona Sul", 2, 15));
        turmas.save(new Turma(meu, dados.motorista(), "Van 3", 3, 15));
        Turma vanAntiga = turmas.save(new Turma(removido, dados.motorista(), "Van antiga", 1, 15));
        Usuario aluno = dados.aluno();
        vinculos.save(new TurmaAluno(minhaVan, aluno));
        TurmaAluno vinculoInativo = new TurmaAluno(vanAntiga, aluno);
        vinculoInativo.setAtivo(false);
        vinculos.save(vinculoInativo);

        mvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, dados.bearer(aluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(meu.getId()))
                .andExpect(jsonPath("$[0].totalTurmas").value(2))
                .andExpect(jsonPath("$[0].minhaTurma.id").value(minhaVan.getId()))
                .andExpect(jsonPath("$[0].minhaTurma.nome").value("Van da Zona Sul"));
        mvc.perform(get("/grupos/{id}", meu.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(aluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minhaTurma.nome").value("Van da Zona Sul"));
        mvc.perform(get("/grupos/{id}", removido.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(aluno)))
                .andExpect(status().isForbidden());
    }

    @Test
    void motoristaVeOsGruposEmQueConduzComSuaTurma() throws Exception {
        Grupo meu = grupo();
        Grupo outro = grupo();
        Usuario motorista = dados.motorista();
        Turma minhaVan = turmas.save(new Turma(meu, motorista, "Van do Carlos", 1, 15));
        turmas.save(new Turma(outro, dados.motorista(), "Van de outro", 2, 15));

        mvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, dados.bearer(motorista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(meu.getId()))
                .andExpect(jsonPath("$[0].totalTurmas").value(1))
                .andExpect(jsonPath("$[0].minhaTurma.id").value(minhaVan.getId()));
        mvc.perform(get("/grupos/{id}", outro.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(motorista)))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalharGrupoInexistenteDevolve404() throws Exception {
        mvc.perform(get("/grupos/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarSemTokenDevolve401() throws Exception {
        mvc.perform(get("/grupos")).andExpect(status().isUnauthorized());
    }

    private Grupo grupo() {
        return grupos.save(new Grupo("Grupo", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
    }

    private ResultActions criar(String bearer, String corpo) throws Exception {
        return mvc.perform(post("/admin/grupos").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(String nome, String periodo, String ida, String volta) {
        return """
                {"nome": "%s", "periodo": "%s", "horarioSaidaIda": "%s", "horarioSaidaVolta": "%s"}
                """.formatted(nome, periodo, ida, volta);
    }

    private static Long idDe(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
