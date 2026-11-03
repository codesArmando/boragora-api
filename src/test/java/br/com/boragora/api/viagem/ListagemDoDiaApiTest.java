package br.com.boragora.api.viagem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.RelogioDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.seguranca.UsuarioAutenticado;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;

/**
 * Grupo "Noite" (ida 18:10, volta 22:30) com a turma "Van 2" do Carlos e a "Van 1" do Vanderlei.
 * Os planos são declarados pelo {@link PlanoDoDiaService}, com o relógio fixado, e as consultas
 * são feitas às 19:00 do mesmo dia.
 */
@TesteDeIntegracao
class ListagemDoDiaApiTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 5);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private RelogioDeTeste relogio;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;
    @Autowired
    private ViagemRepository viagens;
    @Autowired
    private PlanoDoDiaService planos;

    private Grupo grupo;
    private Turma van2;
    private Turma van1;
    private Usuario carlos;
    private Usuario vanderlei;

    @BeforeEach
    void montarGrupo() {
        as("17:00");
        grupo = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
        carlos = dados.motorista("Carlos");
        vanderlei = dados.motorista("Vanderlei");
        van2 = turmas.save(new Turma(grupo, carlos, "Van 2", 2, 15));
        van1 = turmas.save(new Turma(grupo, vanderlei, "Van 1", 1, 15));
    }

    @AfterEach
    void voltarAoRelogioReal() {
        relogio.voltarAoReal();
    }

    @Test
    void turmaMostraContadoresEListasComOCanceladoTardioDestacadoEForaDaContagem() throws Exception {
        Usuario alvaro = aluno(van2, "Álvaro");
        Usuario bruno = aluno(van2, "Bruno");
        Usuario carla = aluno(van2, "Carla");
        Usuario davi = aluno(van2, "Davi");
        aluno(van2, "Eva");
        Usuario fabio = aluno(van2, "Fábio");
        Usuario gil = aluno(van2, "Gil");

        as("17:00");
        ida(alvaro, van2, true);
        volta(alvaro, van2, true);
        ida(bruno, van2, true);
        ida(carla, van2, true);
        ida(fabio, van2, true);
        volta(fabio, van2, true);
        ida(gil, van2, true);
        as("17:30");
        ida(gil, van2, false);
        as("18:30");
        ida(carla, van2, false);
        as("19:00");
        volta(davi, van2, true);
        as("21:00");
        planos.alterarStatusVolta(UsuarioAutenticado.de(alvaro), van2.getId(), StatusVolta.LIBERADO);
        desativar(fabio, van2);

        hojeDaTurma(dados.bearer(carlos), van2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(van2.getId()))
                .andExpect(jsonPath("$.nome").value("Van 2"))
                .andExpect(jsonPath("$.grupo.horarioSaidaIda").value("18:10"))
                .andExpect(jsonPath("$.motorista.nome").value("Carlos"))
                .andExpect(jsonPath("$.semConfirmacoes").value(false))
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(2))
                .andExpect(jsonPath("$.contadores.canceladosAposSaida").value(1))
                .andExpect(jsonPath("$.contadores.voltam").value(2))
                .andExpect(jsonPath("$.contadores.liberados").value(1))
                // Cancelado tardio primeiro, depois ordem alfabética (Álvaro antes de Bruno).
                .andExpect(jsonPath("$.listaIda.length()").value(3))
                .andExpect(jsonPath("$.listaIda[0].alunoId").value(carla.getId()))
                .andExpect(jsonPath("$.listaIda[0].situacao").value("CANCELADO_APOS_SAIDA"))
                .andExpect(jsonPath("$.listaIda[1].nome").value(alvaro.getNomePessoa()))
                .andExpect(jsonPath("$.listaIda[1].situacao").value("CONFIRMADO"))
                .andExpect(jsonPath("$.listaIda[2].alunoId").value(bruno.getId()))
                // Liberados primeiro.
                .andExpect(jsonPath("$.listaVolta.length()").value(2))
                .andExpect(jsonPath("$.listaVolta[0].alunoId").value(alvaro.getId()))
                .andExpect(jsonPath("$.listaVolta[0].statusVolta").value("LIBERADO"))
                .andExpect(jsonPath("$.listaVolta[1].alunoId").value(davi.getId()))
                .andExpect(jsonPath("$.listaVolta[1].statusVolta").value("EM_AULA"));
    }

    @Test
    void listaDaVoltaPoeLiberadosPrimeiroEDepoisOrdemAlfabetica() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        Usuario beto = aluno(van2, "Beto");
        Usuario zeca = aluno(van2, "Zeca");
        as("19:00");
        for (Usuario aluno : List.of(ana, beto, zeca)) {
            volta(aluno, van2, true);
        }
        planos.alterarStatusVolta(UsuarioAutenticado.de(zeca), van2.getId(), StatusVolta.LIBERADO);

        String json = conteudo(hojeDaTurma(dados.bearer(carlos), van2));
        assertThat(JsonPath.<List<Integer>>read(json, "$.listaVolta[*].alunoId")).containsExactly(
                zeca.getId().intValue(), ana.getId().intValue(), beto.getId().intValue());
        assertThat(JsonPath.<Integer>read(json, "$.contadores.liberados")).isEqualTo(1);
        assertThat(JsonPath.<List<?>>read(json, "$.listaIda")).isEmpty();
    }

    @Test
    void turmaSemConfirmacoesDevolveSemConfirmacoesEListasVaziasSemCriarViagem() throws Exception {
        aluno(van2, "Ana");
        as("19:00");

        hojeDaTurma(dados.bearer(carlos), van2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semConfirmacoes").value(true))
                .andExpect(jsonPath("$.listaIda.length()").value(0))
                .andExpect(jsonPath("$.listaVolta.length()").value(0))
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(0))
                .andExpect(jsonPath("$.contadores.voltam").value(0));
        mvc.perform(get("/grupos/{id}/hoje", grupo.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(carlos)))
                .andExpect(jsonPath("$.turmas[0].semConfirmacoes").value(true));
        mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(carlos)))
                .andExpect(jsonPath("$[0].semConfirmacoes").value(true));

        assertThat(viagens.findByTurmaIdAndData(van2.getId(), DIA)).isEmpty();
    }

    @Test
    void viagemSoComCancelamentosAntesDaSaidaContaComoSemConfirmacoes() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        as("17:00");
        ida(ana, van2, true);
        ida(ana, van2, false);
        as("19:00");

        hojeDaTurma(dados.bearer(carlos), van2)
                .andExpect(jsonPath("$.semConfirmacoes").value(true))
                .andExpect(jsonPath("$.listaIda.length()").value(0));
    }

    @Test
    void canceladoTardioSozinhoMantemSemConfirmacoesFalsoParaOMotoristaVerOAviso() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        as("17:00");
        ida(ana, van2, true);
        as("18:30");
        ida(ana, van2, false);

        hojeDaTurma(dados.bearer(carlos), van2)
                .andExpect(jsonPath("$.semConfirmacoes").value(false))
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(0))
                .andExpect(jsonPath("$.contadores.canceladosAposSaida").value(1))
                .andExpect(jsonPath("$.listaIda[0].situacao").value("CANCELADO_APOS_SAIDA"));
    }

    @Test
    void motoristaVeAsSuasTurmasComContadoresEmOrdemDeGrupo() throws Exception {
        Grupo manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 20), LocalTime.of(12, 10)));
        Turma daManha = turmas.save(new Turma(manha, carlos, "Van 9", 9, 15));
        Usuario ana = aluno(van2, "Ana");
        as("17:00");
        ida(ana, van2, true);
        volta(ana, van2, true);

        mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(carlos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(daManha.getId()))
                .andExpect(jsonPath("$[0].semConfirmacoes").value(true))
                .andExpect(jsonPath("$[1].id").value(van2.getId()))
                .andExpect(jsonPath("$[1].nome").value("Van 2"))
                .andExpect(jsonPath("$[1].grupo.id").value(grupo.getId()))
                .andExpect(jsonPath("$[1].grupo.horarioSaidaVolta").value("22:30"))
                .andExpect(jsonPath("$[1].motorista.id").value(carlos.getId()))
                .andExpect(jsonPath("$[1].contadores.vaoNaIda").value(1))
                .andExpect(jsonPath("$[1].contadores.voltam").value(1))
                .andExpect(jsonPath("$[1].semConfirmacoes").value(false))
                .andExpect(jsonPath("$[1].listaIda").doesNotExist());
    }

    @Test
    void chefeQueNaoConduzTurmaRecebeListaVaziaEAlunoRecebe403() throws Exception {
        mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoConsultaOProprioGrupoComSeloVoceSoNoSeuItemE403ForaDele() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        Usuario beto = aluno(van2, "Beto");
        Usuario hugo = aluno(van1, "Hugo");
        as("17:00");
        for (Usuario aluno : List.of(ana, beto)) {
            ida(aluno, van2, true);
            volta(aluno, van2, true);
        }
        ida(hugo, van1, true);
        String bearerAna = dados.bearer(ana);

        String json = conteudo(mvc.perform(get("/grupos/{id}/hoje", grupo.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerAna)));
        assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(grupo.getId().intValue());
        assertThat(JsonPath.<String>read(json, "$.periodo")).isEqualTo("NOITE");
        assertThat(JsonPath.<Integer>read(json, "$.minhaTurma.id")).isEqualTo(van2.getId().intValue());
        // Turmas em ordem de nome: Van 1, Van 2.
        assertThat(JsonPath.<List<Integer>>read(json, "$.turmas[*].id"))
                .containsExactly(van1.getId().intValue(), van2.getId().intValue());
        assertThat(JsonPath.<List<Boolean>>read(json, "$.turmas[*].minhaTurma")).containsExactly(false, true);
        assertThat(JsonPath.<String>read(json, "$.turmas[0].motorista.nome")).isEqualTo("Vanderlei");
        assertThat(JsonPath.<List<Boolean>>read(json, "$.turmas[0].listaIda[*].voce")).containsExactly(false);
        assertThat(JsonPath.<List<Integer>>read(json, "$.turmas[1].listaIda[?(@.voce == true)].alunoId"))
                .containsExactly(ana.getId().intValue());
        assertThat(JsonPath.<List<Integer>>read(json, "$.turmas[1].listaVolta[?(@.voce == true)].alunoId"))
                .containsExactly(ana.getId().intValue());
        assertThat(JsonPath.<List<Boolean>>read(json, "$.turmas[1].listaIda[*].voce")).containsExactly(true, false);

        // O aluno também vê as listas da turma vizinha do mesmo grupo (RN10).
        hojeDaTurma(bearerAna, van1).andExpect(status().isOk());

        Grupo outro = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 20), LocalTime.of(12, 10)));
        Turma deFora = turmas.save(new Turma(outro, dados.motorista(), "Van 5", 5, 15));
        mvc.perform(get("/grupos/{id}/hoje", outro.getId()).header(HttpHeaders.AUTHORIZATION, bearerAna))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        hojeDaTurma(bearerAna, deFora).andExpect(status().isForbidden());
    }

    @Test
    void alunoComVinculoInativoPerdeAcessoAoGrupo() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        desativar(ana, van2);
        mvc.perform(get("/grupos/{id}/hoje", grupo.getId()).header(HttpHeaders.AUTHORIZATION, dados.bearer(ana)))
                .andExpect(status().isForbidden());
    }

    @Test
    void motoristaConsultaTurmasDoGrupoEmQueAtuaE403ForaDele() throws Exception {
        String bearerCarlos = dados.bearer(carlos);
        hojeDaTurma(bearerCarlos, van2).andExpect(status().isOk());
        hojeDaTurma(bearerCarlos, van1).andExpect(status().isOk());
        mvc.perform(get("/grupos/{id}/hoje", grupo.getId()).header(HttpHeaders.AUTHORIZATION, bearerCarlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minhaTurma.id").value(van2.getId()))
                .andExpect(jsonPath("$.turmas[1].minhaTurma").value(true))
                .andExpect(jsonPath("$.turmas[0].minhaTurma").value(false));

        Grupo outro = grupos.save(new Grupo("Tarde", Periodo.TARDE, LocalTime.of(13, 0), LocalTime.of(17, 0)));
        Turma deFora = turmas.save(new Turma(outro, dados.motorista(), "Van 7", 7, 15));
        hojeDaTurma(bearerCarlos, deFora).andExpect(status().isForbidden());
        mvc.perform(get("/grupos/{id}/hoje", outro.getId()).header(HttpHeaders.AUTHORIZATION, bearerCarlos))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaQualquerTurmaEQualquerGrupo() throws Exception {
        String admin = dados.bearer(dados.admin());
        Grupo outro = grupos.save(new Grupo("Tarde", Periodo.TARDE, LocalTime.of(13, 0), LocalTime.of(17, 0)));
        Turma deOutro = turmas.save(new Turma(outro, dados.motorista(), "Van 7", 7, 15));

        hojeDaTurma(admin, van2).andExpect(status().isOk());
        hojeDaTurma(admin, deOutro).andExpect(status().isOk());
        mvc.perform(get("/grupos/{id}/hoje", outro.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minhaTurma").isEmpty())
                .andExpect(jsonPath("$.turmas[0].minhaTurma").value(false));
    }

    @Test
    void turmaOuGrupoInexistenteDevolve404ESemTokenDevolve401() throws Exception {
        String admin = dados.bearer(dados.admin());
        mvc.perform(get("/turmas/{id}/hoje", 999_999_999L).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mvc.perform(get("/grupos/{id}/hoje", 999_999_999L).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mvc.perform(get("/turmas/{id}/hoje", van2.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void listasMostramSoODiaDeHoje() throws Exception {
        Usuario ana = aluno(van2, "Ana");
        as("17:00");
        ida(ana, van2, true);
        relogio.fixar(DIA.plusDays(1).atTime(9, 0));

        hojeDaTurma(dados.bearer(carlos), van2)
                .andExpect(jsonPath("$.semConfirmacoes").value(true))
                .andExpect(jsonPath("$.listaIda.length()").value(0));
    }

    private void as(String horario) {
        relogio.fixar(LocalDateTime.of(DIA, LocalTime.parse(horario)));
    }

    private Usuario aluno(Turma turma, String nome) {
        Usuario aluno = dados.aluno(nome);
        vinculos.save(new TurmaAluno(turma, aluno));
        return aluno;
    }

    private void desativar(Usuario aluno, Turma turma) {
        TurmaAluno vinculo = vinculos.findVinculoAtivo(aluno.getId(), turma.getId()).orElseThrow();
        vinculo.setAtivo(false);
        vinculos.save(vinculo);
    }

    private void ida(Usuario aluno, Turma turma, boolean incluir) {
        planos.alterarIda(UsuarioAutenticado.de(aluno), turma.getId(), incluir);
    }

    private void volta(Usuario aluno, Turma turma, boolean incluir) {
        planos.alterarVolta(UsuarioAutenticado.de(aluno), turma.getId(), incluir);
    }

    private ResultActions hojeDaTurma(String bearer, Turma turma) throws Exception {
        return mvc.perform(get("/turmas/{id}/hoje", turma.getId()).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private static String conteudo(ResultActions resultado) throws Exception {
        return resultado.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
