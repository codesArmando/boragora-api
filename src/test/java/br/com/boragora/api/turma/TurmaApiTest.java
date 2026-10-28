package br.com.boragora.api.turma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.viagem.Confirmacao;
import br.com.boragora.api.viagem.ConfirmacaoRepository;
import br.com.boragora.api.viagem.Viagem;
import br.com.boragora.api.viagem.ViagemRepository;
import com.jayway.jsonpath.JsonPath;

@TesteDeIntegracao
class TurmaApiTest {

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
    private ViagemRepository viagens;
    @Autowired
    private ConfirmacaoRepository confirmacoes;
    @Autowired
    private UsuarioRepository usuarios;

    private String admin;

    @BeforeEach
    void entrarComoAdmin() {
        admin = dados.bearer(dados.admin());
    }

    // --- CRUD do admin ---

    @Test
    void adminCriaTurmaAlteraDadosCadastraisETrocaOMotorista() throws Exception {
        Grupo grupo = grupo();
        Usuario carlos = dados.motorista("Carlos");
        Usuario vanderlei = dados.motorista("Vanderlei");

        Long id = idDe(criar(admin, corpoCriacao(grupo.getId(), "  Van da Zona Sul ", 2, 15, carlos.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Van da Zona Sul"))
                .andExpect(jsonPath("$.numeroVan").value(2))
                .andExpect(jsonPath("$.capacidade").value(15))
                .andExpect(jsonPath("$.grupo.id").value(grupo.getId()))
                .andExpect(jsonPath("$.grupo.nome").value("Noite"))
                .andExpect(jsonPath("$.grupo.periodo").value("NOITE"))
                .andExpect(jsonPath("$.grupo.horarioSaidaIda").value("18:10"))
                .andExpect(jsonPath("$.grupo.horarioSaidaVolta").value("22:30"))
                .andExpect(jsonPath("$.motorista.id").value(carlos.getId()))
                .andExpect(jsonPath("$.motorista.nome").value("Carlos"))
                .andExpect(jsonPath("$.alunos.length()").value(0)));

        editar(admin, id, corpoEdicao("Van 7", 7, 20, vanderlei.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Van 7"))
                .andExpect(jsonPath("$.numeroVan").value(7))
                .andExpect(jsonPath("$.capacidade").value(20))
                .andExpect(jsonPath("$.motorista.id").value(vanderlei.getId()))
                .andExpect(jsonPath("$.grupo.id").value(grupo.getId()));

        Turma gravada = turmas.findById(id).orElseThrow();
        assertThat(gravada.getNome()).isEqualTo("Van 7");
        assertThat(gravada.getNumeroVan()).isEqualTo(7);
        assertThat(gravada.getCapacidade()).isEqualTo(20);
    }

    @Test
    void numeroDaVanECapacidadeSaoOpcionais() throws Exception {
        Grupo grupo = grupo();
        Long id = idDe(criar(admin, """
                {"grupoId": %d, "nome": "Van sem dados", "motoristaId": %d}
                """.formatted(grupo.getId(), dados.motorista().getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVan").doesNotExist())
                .andExpect(jsonPath("$.capacidade").doesNotExist()));

        Usuario motorista = turmas.findById(id).orElseThrow().getMotorista();
        editar(admin, id, """
                {"nome": "Van sem dados", "numeroVan": 3, "motoristaId": %d}
                """.formatted(motorista.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVan").value(3))
                .andExpect(jsonPath("$.capacidade").doesNotExist());
    }

    @Test
    void alterarONomePelaEdicaoRefleteNoDetalhe() throws Exception {
        Turma turma = turma(grupo(), dados.motorista(), "Van 2");

        editar(admin, turma.getId(), corpoEdicao("Van da Zona Sul", 2, 15, turma.getMotorista().getId()))
                .andExpect(status().isOk());

        mvc.perform(get("/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Van da Zona Sul"));
    }

    @Test
    void naoAdminRecebe403NasRotasAdminTurmas() throws Exception {
        Grupo grupo = grupo();
        Usuario motorista = dados.motorista();
        Turma turma = turma(grupo, motorista, "Van 1");
        for (String bearer : new String[] { dados.bearer(dados.aluno()), dados.bearer(motorista) }) {
            mvc.perform(get("/admin/turmas").header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            criar(bearer, corpoCriacao(grupo.getId(), "Van X", 1, 15, dados.motorista().getId()))
                    .andExpect(status().isForbidden());
            editar(bearer, turma.getId(), corpoEdicao("Van X", 1, 15, motorista.getId()))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/admin/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden());
        }
        assertThat(turmas.findById(turma.getId()).orElseThrow().getNome()).isEqualTo("Van 1");
    }

    // --- RN08 para motoristas ---

    @Test
    void mesmoMotoristaEmDuasTurmasDoMesmoGrupoDevolve409NaCriacao() throws Exception {
        Grupo grupo = grupo();
        Usuario motorista = dados.motorista();
        turma(grupo, motorista, "Van 1");

        criar(admin, corpoCriacao(grupo.getId(), "Van 2", 2, 15, motorista.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_GRUPO_MOTORISTA"))
                .andExpect(jsonPath("$.mensagem").value("Este motorista já conduz a turma \"Van 1\" neste grupo."));
        assertThat(turmas.findByMotoristaId(motorista.getId())).hasSize(1);
    }

    @Test
    void mesmoMotoristaEmDuasTurmasDoMesmoGrupoDevolve409NaTroca() throws Exception {
        Grupo grupo = grupo();
        Usuario carlos = dados.motorista();
        Usuario vanderlei = dados.motorista();
        turma(grupo, vanderlei, "Van 1");
        Turma daCarlos = turma(grupo, carlos, "Van 2");

        editar(admin, daCarlos.getId(), corpoEdicao("Van 2 nova", 2, 15, vanderlei.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_GRUPO_MOTORISTA"));

        Turma gravada = turmas.findById(daCarlos.getId()).orElseThrow();
        assertThat(gravada.getMotorista().getId()).isEqualTo(carlos.getId());
        assertThat(gravada.getNome()).isEqualTo("Van 2");
    }

    @Test
    void editarSemTrocarOMotoristaNaoAcusaConflito() throws Exception {
        Turma turma = turma(grupo(), dados.motorista(), "Van 1");
        editar(admin, turma.getId(), corpoEdicao("Van 1b", 1, 15, turma.getMotorista().getId()))
                .andExpect(status().isOk());
    }

    @Test
    void mesmoMotoristaEmTurmasDeGruposDiferentesEAceito() throws Exception {
        Usuario vanderlei = dados.motorista("Vanderlei");
        Grupo manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 30), LocalTime.of(12, 0)));
        Grupo noite = grupo();

        criar(admin, corpoCriacao(manha.getId(), "Van 1", 1, 15, vanderlei.getId())).andExpect(status().isCreated());
        criar(admin, corpoCriacao(noite.getId(), "Van 1", 1, 15, vanderlei.getId())).andExpect(status().isCreated());

        Turma outra = turma(grupo(), dados.motorista(), "Van 3");
        editar(admin, outra.getId(), corpoEdicao("Van 3", 3, 15, vanderlei.getId())).andExpect(status().isOk());
        assertThat(turmas.findByMotoristaId(vanderlei.getId())).hasSize(3);
    }

    // --- validação ---

    @Test
    void turmaSemNomeEmBrancoOuComMaisDe60CaracteresDevolve400() throws Exception {
        Grupo grupo = grupo();
        Long motoristaId = dados.motorista().getId();
        String longo = "x".repeat(61);

        criar(admin, """
                {"grupoId": %d, "motoristaId": %d}
                """.formatted(grupo.getId(), motoristaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists());
        criar(admin, corpoCriacao(grupo.getId(), "   ", 1, 15, motoristaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
        criar(admin, corpoCriacao(grupo.getId(), longo, 1, 15, motoristaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());

        Turma turma = turma(grupo, dados.motorista(), "Van 1");
        editar(admin, turma.getId(), corpoEdicao(" ", 1, 15, turma.getMotorista().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
        editar(admin, turma.getId(), corpoEdicao(longo, 1, 15, turma.getMotorista().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
        assertThat(turmas.findById(turma.getId()).orElseThrow().getNome()).isEqualTo("Van 1");
    }

    @Test
    void nomeCom60CaracteresDepoisDeTirarOsEspacosEAceito() throws Exception {
        String sessenta = "x".repeat(60);
        criar(admin, corpoCriacao(grupo().getId(), "  " + sessenta + "  ", 1, 15, dados.motorista().getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(sessenta));
    }

    @Test
    void mesmoNomeEMesmoNumeroDaVanNoMesmoGrupoSaoAceitos() throws Exception {
        Grupo grupo = grupo();
        criar(admin, corpoCriacao(grupo.getId(), "Van 2", 2, 15, dados.motorista().getId()))
                .andExpect(status().isCreated());
        criar(admin, corpoCriacao(grupo.getId(), "Van 2", 2, 15, dados.motorista().getId()))
                .andExpect(status().isCreated());
        assertThat(turmas.findAll().stream().filter(t -> t.getGrupo().getId().equals(grupo.getId()))).hasSize(2);
    }

    @Test
    void camposObrigatoriosENumerosInvalidosDevolvem400() throws Exception {
        criar(admin, "{\"nome\": \"Van\", \"numeroVan\": 0, \"capacidade\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.grupoId").exists())
                .andExpect(jsonPath("$.campos.motoristaId").exists())
                .andExpect(jsonPath("$.campos.numeroVan").exists())
                .andExpect(jsonPath("$.campos.capacidade").exists());
    }

    @Test
    void motoristaIdPrecisaSerMotoristaAtivo() throws Exception {
        Grupo grupo = grupo();
        Usuario inativo = dados.motorista();
        inativo.setAtivo(false);
        usuarios.save(inativo);

        for (Long id : new Long[] { dados.aluno().getId(), inativo.getId(), Long.MAX_VALUE }) {
            criar(admin, corpoCriacao(grupo.getId(), "Van 1", 1, 15, id))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                    .andExpect(jsonPath("$.campos.motoristaId").exists());
        }

        Turma turma = turma(grupo, dados.motorista(), "Van 1");
        editar(admin, turma.getId(), corpoEdicao("Van 1", 1, 15, dados.aluno().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motoristaId").exists());
    }

    @Test
    void grupoInexistenteNaCriacaoDevolve400() throws Exception {
        criar(admin, corpoCriacao(Long.MAX_VALUE, "Van 1", 1, 15, dados.motorista().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.grupoId").exists());
    }

    @Test
    void editarExcluirOuDetalharTurmaInexistenteDevolve404() throws Exception {
        editar(admin, Long.MAX_VALUE, corpoEdicao("Van", 1, 15, dados.motorista().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        mvc.perform(delete("/admin/turmas/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mvc.perform(get("/turmas/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    // --- exclusão ---

    @Test
    void excluirTurmaDevolve204ERemoveVinculosViagensEConfirmacoes() throws Exception {
        Grupo grupo = grupo();
        Usuario motorista = dados.motorista();
        Usuario aluno = dados.aluno();
        Turma turma = turma(grupo, motorista, "Van 1");
        TurmaAluno vinculo = vinculos.save(new TurmaAluno(turma, aluno));
        Viagem viagem = viagens.save(new Viagem(turma, LocalDate.of(2026, 10, 19)));
        Confirmacao confirmacao = confirmacoes.save(new Confirmacao(viagem, vinculo, LocalDateTime.of(2026, 10, 19, 17, 0)));

        mvc.perform(delete("/admin/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        assertThat(turmas.existsById(turma.getId())).isFalse();
        assertThat(vinculos.existsById(vinculo.getId())).isFalse();
        assertThat(viagens.existsById(viagem.getId())).isFalse();
        assertThat(confirmacoes.existsById(confirmacao.getId())).isFalse();
        assertThat(usuarios.existsById(motorista.getId())).isTrue();
        assertThat(usuarios.existsById(aluno.getId())).isTrue();
        mvc.perform(get("/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/turmas").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[*].id", not(hasItem(turma.getId().intValue()))));
    }

    @Test
    void depoisDaExclusaoMotoristaEAlunosPodemEntrarEmOutraTurmaDoMesmoGrupo() throws Exception {
        Grupo grupo = grupo();
        Usuario motorista = dados.motorista();
        Usuario aluno = dados.aluno();
        Turma turma = turma(grupo, motorista, "Van 1");
        vinculos.save(new TurmaAluno(turma, aluno));

        mvc.perform(delete("/admin/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        Long novaId = idDe(criar(admin, corpoCriacao(grupo.getId(), "Van nova", 1, 15, motorista.getId()))
                .andExpect(status().isCreated()));
        // O vínculo do aluno não existe mais, então nada o prende ao grupo (RN08 dos alunos, etapa 5).
        assertThat(vinculos.existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(aluno.getId(), grupo.getId())).isFalse();
        vinculos.saveAndFlush(new TurmaAluno(turmas.findById(novaId).orElseThrow(), aluno));
        mvc.perform(get("/turmas/{id}", novaId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()));
    }

    @Test
    void excluirGrupoDepoisDeExcluirSuasTurmasFunciona() throws Exception {
        Grupo grupo = grupo();
        Turma turma = turma(grupo, dados.motorista(), "Van 1");

        mvc.perform(delete("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("GRUPO_COM_TURMAS"));
        mvc.perform(delete("/admin/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/admin/grupos/{id}", grupo.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
    }

    // --- GET /turmas/{id} ---

    @Test
    void detalheTrazSoAlunosAtivosEmOrdemAlfabetica() throws Exception {
        Turma turma = turma(grupo(), dados.motorista("Carlos"), "Van 1");
        Usuario joao = dados.aluno("João");
        Usuario ana = dados.aluno("Ana");
        Usuario removido = dados.aluno("Bruno");
        Usuario beatriz = dados.aluno("beatriz");
        vinculos.save(new TurmaAluno(turma, joao));
        vinculos.save(new TurmaAluno(turma, ana));
        vinculos.save(new TurmaAluno(turma, beatriz));
        TurmaAluno inativo = new TurmaAluno(turma, removido);
        inativo.setAtivo(false);
        vinculos.save(inativo);

        mvc.perform(get("/turmas/{id}", turma.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motorista.nome").value("Carlos"))
                .andExpect(jsonPath("$.alunos.length()").value(3))
                .andExpect(jsonPath("$.alunos[0].nome").value("Ana"))
                .andExpect(jsonPath("$.alunos[1].nome").value("beatriz"))
                .andExpect(jsonPath("$.alunos[2].nome").value("João"))
                .andExpect(jsonPath("$.alunos[2].id").value(joao.getId()))
                .andExpect(jsonPath("$.alunos[0].email").doesNotExist());
    }

    @Test
    void detalheRespeitaAVisibilidade() throws Exception {
        Grupo meu = grupo();
        Grupo outro = grupo();
        Usuario motorista = dados.motorista();
        Usuario aluno = dados.aluno();
        Turma minha = turma(meu, motorista, "Van 1");
        Turma vizinha = turma(meu, dados.motorista(), "Van 2");
        Turma deFora = turma(outro, dados.motorista(), "Van 3");
        vinculos.save(new TurmaAluno(minha, aluno));

        for (Usuario usuario : new Usuario[] { aluno, motorista }) {
            String bearer = dados.bearer(usuario);
            mvc.perform(get("/turmas/{id}", minha.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk());
            mvc.perform(get("/turmas/{id}", vizinha.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk());
            mvc.perform(get("/turmas/{id}", deFora.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        }
        mvc.perform(get("/turmas/{id}", minha.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/turmas/{id}", deFora.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mvc.perform(get("/turmas/{id}", minha.getId())).andExpect(status().isUnauthorized());
    }

    // --- GET /admin/turmas ---

    @Test
    void listaCadastralTrazTodasAsTurmasComTotalDeAlunosAtivos() throws Exception {
        Grupo grupo = grupo();
        Usuario motorista = dados.motorista("Carlos");
        Turma turma = turmas.save(new Turma(grupo, motorista, "Van 1", 4, 15));
        Turma vazia = turma(grupo, dados.motorista(), "Van 2");
        vinculos.save(new TurmaAluno(turma, dados.aluno()));
        vinculos.save(new TurmaAluno(turma, dados.aluno()));
        TurmaAluno inativo = new TurmaAluno(turma, dados.aluno());
        inativo.setAtivo(false);
        vinculos.save(inativo);

        String json = mvc.perform(get("/admin/turmas").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<Integer>read(json, "$.length()")).isEqualTo((int) turmas.count());
        String item = "$[?(@.id == %d)]".formatted(turma.getId());
        assertThat(JsonPath.<List<Object>>read(json, item + ".nome")).containsExactly("Van 1");
        assertThat(JsonPath.<List<Object>>read(json, item + ".numeroVan")).containsExactly(4);
        assertThat(JsonPath.<List<Object>>read(json, item + ".capacidade")).containsExactly(15);
        assertThat(JsonPath.<List<Object>>read(json, item + ".totalAlunos")).containsExactly(2);
        assertThat(JsonPath.<List<Object>>read(json, item + ".grupo.id")).containsExactly(grupo.getId().intValue());
        assertThat(JsonPath.<List<Object>>read(json, item + ".grupo.periodo")).containsExactly("NOITE");
        assertThat(JsonPath.<List<Object>>read(json, item + ".grupo.horarioSaidaIda")).containsExactly("18:10");
        assertThat(JsonPath.<List<Object>>read(json, item + ".motorista.id")).containsExactly(motorista.getId().intValue());
        assertThat(JsonPath.<List<Object>>read(json, item + ".motorista.nome")).containsExactly("Carlos");
        assertThat(JsonPath.<List<Object>>read(json, "$[?(@.id == %d)].totalAlunos".formatted(vazia.getId())))
                .containsExactly(0);
    }

    @Test
    void listaCadastralSegueAOrdemDosGruposEDepoisONomeDaTurma() throws Exception {
        Grupo noite = grupo();
        Grupo manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 30), LocalTime.of(12, 0)));
        Turma noiteB = turma(noite, dados.motorista(), "b da noite");
        Turma noiteA = turma(noite, dados.motorista(), "A da noite");
        Turma manhaZ = turma(manha, dados.motorista(), "Z da manhã");

        String json = mvc.perform(get("/admin/turmas").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).containsSubsequence(manhaZ.getId().intValue(), noiteA.getId().intValue(),
                noiteB.getId().intValue());
        // Turmas do mesmo grupo ficam juntas.
        assertThat(ids.indexOf(noiteB.getId().intValue()) - ids.indexOf(noiteA.getId().intValue())).isEqualTo(1);
    }

    private Grupo grupo() {
        return grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
    }

    private Turma turma(Grupo grupo, Usuario motorista, String nome) {
        return turmas.save(new Turma(grupo, motorista, nome, 1, 15));
    }

    private ResultActions criar(String bearer, String corpo) throws Exception {
        return mvc.perform(post("/admin/turmas").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions editar(String bearer, Long id, String corpo) throws Exception {
        return mvc.perform(put("/admin/turmas/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpoCriacao(Long grupoId, String nome, Integer numeroVan, Integer capacidade,
            Long motoristaId) {
        return """
                {"grupoId": %d, "nome": "%s", "numeroVan": %d, "capacidade": %d, "motoristaId": %d}
                """.formatted(grupoId, nome, numeroVan, capacidade, motoristaId);
    }

    private static String corpoEdicao(String nome, Integer numeroVan, Integer capacidade, Long motoristaId) {
        return """
                {"nome": "%s", "numeroVan": %d, "capacidade": %d, "motoristaId": %d}
                """.formatted(nome, numeroVan, capacidade, motoristaId);
    }

    private static Long idDe(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
