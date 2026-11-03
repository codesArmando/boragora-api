package br.com.boragora.api.turma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;

@TesteDeIntegracao
class VinculoApiTest {

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
    @Autowired
    private UsuarioRepository usuarios;

    private String admin;
    /** Trecho único nos nomes dos alunos de cada teste, para a busca não trazer dados de outros testes. */
    private String marca;

    @BeforeEach
    void entrarComoAdmin() {
        admin = dados.bearer(dados.admin());
        marca = "m" + UUID.randomUUID().toString().substring(0, 8);
    }

    // --- POST /admin/turmas/{id}/alunos ---

    @Test
    void vincularDoisAlunosDeUmaVezEElesAparecemNoDetalhe() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario bruna = aluno("Bruna");
        Usuario ana = aluno("Ana");

        vincular(turma.getId(), ana.getId(), bruna.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turma.getId()))
                .andExpect(jsonPath("$.alunos.length()").value(2));

        mvc.perform(get("/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunos.length()").value(2))
                .andExpect(jsonPath("$.alunos[0].id").value(ana.getId()))
                .andExpect(jsonPath("$.alunos[1].id").value(bruna.getId()));
    }

    @Test
    void alunoEmOutraTurmaDoMesmoGrupoDevolve409ENinguemDoLoteEVinculado() throws Exception {
        Grupo grupo = grupo();
        Turma van1 = turma(grupo, "Van 1");
        Turma van2 = turma(grupo, "Van 2");
        Usuario livre = aluno("Livre");
        Usuario joao = aluno("João");
        vincular(van1.getId(), joao.getId()).andExpect(status().isOk());

        vincular(van2.getId(), livre.getId(), joao.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_GRUPO_ALUNO"))
                .andExpect(jsonPath("$.mensagem").value(containsString(joao.getNomePessoa())))
                .andExpect(jsonPath("$.mensagem").value(containsString("Van 1")));

        assertThat(vinculos.findAlunosAtivosDaTurma(van2.getId())).isEmpty();
        assertThat(vinculos.findByTurmaIdAndAlunoId(van2.getId(), livre.getId())).isEmpty();
    }

    @Test
    void alunoPodeEstarEmTurmasDeGruposDiferentes() throws Exception {
        Turma manha = turma(grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 20), LocalTime.of(12, 10))),
                "Van 1");
        Turma noite = turma(grupo(), "Van 2");
        Usuario joao = aluno("João");

        vincular(manha.getId(), joao.getId()).andExpect(status().isOk());
        vincular(noite.getId(), joao.getId()).andExpect(status().isOk());

        assertThat(vinculos.findTurmasAtivasDoAluno(joao.getId())).extracting(Turma::getId)
                .containsExactlyInAnyOrder(manha.getId(), noite.getId());
    }

    @Test
    void soAlunosAtivosPodemSerVinculados() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario valido = aluno("Válido");
        Usuario inativo = aluno("Inativo");
        inativo.setAtivo(false);
        usuarios.save(inativo);

        for (Long invalido : new Long[] { dados.motorista().getId(), inativo.getId(), 999_999_999L }) {
            vincular(turma.getId(), valido.getId(), invalido)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                    .andExpect(jsonPath("$.campos.alunoIds").value(containsString(invalido.toString())));
        }
        assertThat(vinculos.findAlunosAtivosDaTurma(turma.getId())).isEmpty();
    }

    @Test
    void removerEVincularDeNovoReutilizaOMesmoRegistro() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario aluno = aluno("Aluno");
        vincular(turma.getId(), aluno.getId()).andExpect(status().isOk());
        Long idDoVinculo = vinculos.findByTurmaIdAndAlunoId(turma.getId(), aluno.getId()).orElseThrow().getId();

        remover(turma.getId(), aluno.getId()).andExpect(status().isNoContent());
        assertThat(vinculos.findById(idDoVinculo).orElseThrow().isAtivo()).isFalse();

        vincular(turma.getId(), aluno.getId()).andExpect(status().isOk());
        TurmaAluno reativado = vinculos.findByTurmaIdAndAlunoId(turma.getId(), aluno.getId()).orElseThrow();
        assertThat(reativado.getId()).isEqualTo(idDoVinculo);
        assertThat(reativado.isAtivo()).isTrue();
    }

    @Test
    void vincularQuemJaEstaNaTurmaNaoMudaNada() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario aluno = aluno("Aluno");
        vincular(turma.getId(), aluno.getId()).andExpect(status().isOk());

        vincular(turma.getId(), aluno.getId(), aluno.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunos.length()").value(1));
    }

    @Test
    void depoisDeRemovidoOAlunoPodeEntrarEmOutraTurmaDoMesmoGrupo() throws Exception {
        Grupo grupo = grupo();
        Turma van1 = turma(grupo, "Van 1");
        Turma van2 = turma(grupo, "Van 2");
        Usuario aluno = aluno("Aluno");
        vincular(van1.getId(), aluno.getId()).andExpect(status().isOk());
        remover(van1.getId(), aluno.getId()).andExpect(status().isNoContent());

        vincular(van2.getId(), aluno.getId()).andExpect(status().isOk());
    }

    @Test
    void listaDeAlunosVaziaOuNulaDevolve400() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        for (String corpo : new String[] { "{}", "{\"alunoIds\": []}", "{\"alunoIds\": [null]}" }) {
            mvc.perform(post("/admin/turmas/{id}/alunos", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin)
                    .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        }
    }

    // --- DELETE /admin/turmas/{id}/alunos/{alunoId} ---

    @Test
    void alunoRemovidoDeixaDeAparecerNoDetalhe() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario fica = aluno("Fica");
        Usuario sai = aluno("Sai");
        vincular(turma.getId(), fica.getId(), sai.getId()).andExpect(status().isOk());

        remover(turma.getId(), sai.getId()).andExpect(status().isNoContent());

        mvc.perform(get("/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.alunos.length()").value(1))
                .andExpect(jsonPath("$.alunos[0].id").value(fica.getId()));
    }

    @Test
    void removerAlunoSemVinculoAtivoOuDeTurmaInexistenteDevolve404() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario aluno = aluno("Aluno");

        remover(turma.getId(), aluno.getId()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        vincular(turma.getId(), aluno.getId()).andExpect(status().isOk());
        remover(turma.getId(), aluno.getId()).andExpect(status().isNoContent());
        remover(turma.getId(), aluno.getId()).andExpect(status().isNotFound());

        remover(999_999_999L, aluno.getId()).andExpect(status().isNotFound());
        vincular(999_999_999L, aluno.getId()).andExpect(status().isNotFound());
    }

    // --- GET /admin/alunos ---

    @Test
    void buscaFiltraPorNomeEPorEmailENaoListaQuemJaEstaNaTurma() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario ana = aluno("Ana");
        Usuario bruno = aluno("Bruno");
        Usuario naTurma = aluno("Carla");
        vincular(turma.getId(), naTurma.getId()).andExpect(status().isOk());

        assertThat(ids(listar("busca", marca, "turmaId", turma.getId().toString())))
                .containsExactly(ana.getId(), bruno.getId());
        assertThat(ids(listar("busca", "BRUNO " + marca.toUpperCase()))).containsExactly(bruno.getId());
        assertThat(ids(listar("busca", ana.getEmail().substring(0, 14)))).containsExactly(ana.getId());
        assertThat(ids(listar("busca", marca + "-inexistente"))).isEmpty();
    }

    @Test
    void comTurmaIdTrazASituacaoDeCadaAluno() throws Exception {
        Grupo grupo = grupo();
        Turma van1 = turma(grupo, "Van 1");
        Turma van2 = turma(grupo, "Van 2");
        Turma deOutroGrupo = turma(grupo(), "Van 9");
        Usuario semTurma = aluno("Ana");
        Usuario emConflito = aluno("Bruno");
        Usuario deOutroGrupoAluno = aluno("Carla");
        vincular(van1.getId(), emConflito.getId()).andExpect(status().isOk());
        vincular(deOutroGrupo.getId(), deOutroGrupoAluno.getId()).andExpect(status().isOk());

        String json = listar("busca", marca, "turmaId", van2.getId().toString());

        assertThat(ids(json)).containsExactly(semTurma.getId(), emConflito.getId(), deOutroGrupoAluno.getId());
        assertThat(JsonPath.<String>read(json, "$[0].situacao")).isEqualTo("DISPONIVEL");
        assertThat(JsonPath.<List<?>>read(json, "$[0].turmas")).isEmpty();
        assertThat(JsonPath.<Object>read(json, "$[0].turmaEmConflito")).isNull();
        assertThat(JsonPath.<String>read(json, "$[0].email")).isEqualTo(semTurma.getEmail());

        assertThat(JsonPath.<String>read(json, "$[1].situacao")).isEqualTo("CONFLITO_NO_GRUPO");
        assertThat(JsonPath.<Integer>read(json, "$[1].turmaEmConflito.id")).isEqualTo(van1.getId().intValue());
        assertThat(JsonPath.<String>read(json, "$[1].turmaEmConflito.nome")).isEqualTo("Van 1");

        assertThat(JsonPath.<String>read(json, "$[2].situacao")).isEqualTo("DISPONIVEL");
        assertThat(JsonPath.<String>read(json, "$[2].turmas[0].nome")).isEqualTo("Van 9");
        assertThat(JsonPath.<String>read(json, "$[2].turmas[0].grupo.nome")).isEqualTo("Noite");
    }

    @Test
    void semTurmaIdTrazAlunosComESemVinculoComSituacaoNula() throws Exception {
        Turma turma = turma(grupo(), "Van 1");
        Usuario vinculado = aluno("Ana");
        Usuario semTurma = aluno("Bruno");
        Usuario removido = aluno("Carla");
        vincular(turma.getId(), vinculado.getId(), removido.getId()).andExpect(status().isOk());
        remover(turma.getId(), removido.getId()).andExpect(status().isNoContent());
        Usuario inativo = aluno("Dora");
        inativo.setAtivo(false);
        usuarios.save(inativo);

        String json = listar("busca", marca);

        assertThat(ids(json)).containsExactly(vinculado.getId(), semTurma.getId(), removido.getId());
        assertThat(JsonPath.<Integer>read(json, "$[0].turmas.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(json, "$[0].turmas[0].nome")).isEqualTo("Van 1");
        assertThat(JsonPath.<List<?>>read(json, "$[1].turmas")).isEmpty();
        assertThat(JsonPath.<List<?>>read(json, "$[2].turmas")).isEmpty();
        for (int i = 0; i < 3; i++) {
            assertThat(JsonPath.<Object>read(json, "$[%d].situacao".formatted(i))).isNull();
        }
    }

    @Test
    void listagemGeralTrazSoAlunos() throws Exception {
        String json = listar();
        List<Long> ids = ids(json);
        assertThat(ids).isNotEmpty();
        assertThat(usuarios.findAllById(ids)).allMatch(usuario -> usuario.getPapel().name().equals("ALUNO"));
    }

    @Test
    void turmaIdInexistenteNaListagemDevolve400() throws Exception {
        mvc.perform(get("/admin/alunos").param("turmaId", "999999999").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.turmaId").exists());
    }

    @Test
    void naoAdminRecebe403NasRotasDeVinculo() throws Exception {
        Usuario motorista = dados.motorista();
        Turma turma = turmas.save(new Turma(grupo(), motorista, "Van 1", 1, 15));
        Usuario aluno = aluno("Aluno");
        for (String bearer : new String[] { dados.bearer(aluno), dados.bearer(motorista) }) {
            mvc.perform(get("/admin/alunos").header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/admin/turmas/{id}/alunos", turma.getId()).header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON).content(corpo(aluno.getId())))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/admin/turmas/{id}/alunos/{alunoId}", turma.getId(), aluno.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden());
        }
        assertThat(vinculos.findAlunosAtivosDaTurma(turma.getId())).isEmpty();
    }

    private Grupo grupo() {
        return grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
    }

    private Turma turma(Grupo grupo, String nome) {
        return turmas.save(new Turma(grupo, dados.motorista(), nome, 1, 15));
    }

    /** O nome leva a marca do teste; a ordem alfabética segue o primeiro nome. */
    private Usuario aluno(String nome) {
        return dados.aluno(nome + " " + marca);
    }

    private ResultActions vincular(Long turmaId, Long... alunoIds) throws Exception {
        return mvc.perform(post("/admin/turmas/{id}/alunos", turmaId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(alunoIds)));
    }

    private ResultActions remover(Long turmaId, Long alunoId) throws Exception {
        return mvc.perform(delete("/admin/turmas/{id}/alunos/{alunoId}", turmaId, alunoId)
                .header(HttpHeaders.AUTHORIZATION, admin));
    }

    private String listar(String... parametros) throws Exception {
        var requisicao = get("/admin/alunos").header(HttpHeaders.AUTHORIZATION, admin);
        for (int i = 0; i < parametros.length; i += 2) {
            requisicao.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String corpo(Long... alunoIds) {
        return "{\"alunoIds\": [%s]}".formatted(Stream.of(alunoIds).map(String::valueOf)
                .collect(Collectors.joining(", ")));
    }

    private static List<Long> ids(String json) {
        return JsonPath.<List<Number>>read(json, "$[*].id").stream().map(Number::longValue).toList();
    }
}
