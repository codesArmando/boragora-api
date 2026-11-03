package br.com.boragora.api.viagem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
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

/** Grupo com ida às 18:10 e volta às 22:30, no dia {@link #DIA}, com o relógio fixado em cada passo. */
@TesteDeIntegracao
@RecordApplicationEvents
class PlanoDoDiaApiTest {

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
    private ConfirmacaoRepository confirmacoes;
    @Autowired
    private PlanoDoDiaService service;
    @Autowired
    private ApplicationEvents eventos;

    private Turma turma;
    private Usuario aluno;
    private String bearer;

    @BeforeEach
    void montarTurmaComAluno() {
        as("17:00");
        Grupo grupo = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
        turma = turmas.save(new Turma(grupo, dados.motorista("Carlos"), "Van 2", 2, 15));
        aluno = dados.aluno("João");
        vinculos.save(new TurmaAluno(turma, aluno));
        bearer = dados.bearer(aluno);
    }

    @AfterEach
    void voltarAoRelogioReal() {
        relogio.voltarAoReal();
    }

    // --- Tabela "Pronto quando" da etapa 6 ---

    @Test
    void as1700IncluirIdaEVoltaMarcaAsDuasComStatusEmAulaECriaAViagem() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        volta(true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("2026-10-05"))
                .andExpect(jsonPath("$.turma.id").value(turma.getId()))
                .andExpect(jsonPath("$.turma.nome").value("Van 2"))
                .andExpect(jsonPath("$.turma.grupo.horarioSaidaIda").value("18:10"))
                .andExpect(jsonPath("$.turma.grupo.horarioSaidaVolta").value("22:30"))
                .andExpect(jsonPath("$.incluiIda").value(true))
                .andExpect(jsonPath("$.incluiVolta").value(true))
                .andExpect(jsonPath("$.statusVolta").value("EM_AULA"))
                .andExpect(jsonPath("$.cancelouAposSaida").value(false));

        assertThat(viagens.findByTurmaIdAndData(turma.getId(), DIA)).isPresent();
        Confirmacao confirmacao = confirmacaoDoDia();
        assertThat(confirmacao.getAtualizadoEm()).isEqualTo(DIA.atTime(17, 0));
    }

    @Test
    void as1730CancelarIdaDesmarcaSemCancelamentoTardioNemEvento() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("17:30");
        ida(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiIda").value(false))
                .andExpect(jsonPath("$.cancelouAposSaida").value(false));

        assertThat(eventosPublicados()).isEmpty();
        assertThat(confirmacaoDoDia().getAtualizadoEm()).isEqualTo(DIA.atTime(17, 30));
    }

    @Test
    void as1809IncluirIdaEAceito() throws Exception {
        as("18:09");
        ida(true).andExpect(status().isOk()).andExpect(jsonPath("$.incluiIda").value(true));
    }

    @Test
    void as1810IncluirIdaDevolve422IdaEncerrada() throws Exception {
        as("18:10");
        ida(true)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("IDA_ENCERRADA"));
        assertThat(confirmacoes.findDoDia(vinculo().getId(), DIA)).isEmpty();
    }

    @Test
    void as1830CancelarIdaMarcadaECancelamentoTardioComUmEvento() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("18:30");
        ida(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiIda").value(false))
                .andExpect(jsonPath("$.cancelouAposSaida").value(true));

        Confirmacao confirmacao = confirmacaoDoDia();
        assertThat(eventosPublicados()).containsExactly(
                new IdaCanceladaAposSaida(turma.getId(), aluno.getId(), confirmacao.getId(), DIA));
    }

    @Test
    void as1831IncluirIdaDeNovoDepoisDoCancelamentoTardioDevolve422() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("18:30");
        ida(false).andExpect(status().isOk());
        as("18:31");
        ida(true)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("IDA_ENCERRADA"));
    }

    @Test
    void as1830CancelarIdaNaoMarcadaDevolve200SemAlteracaoNemEvento() throws Exception {
        as("17:00");
        volta(true).andExpect(status().isOk());
        as("18:30");
        ida(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiIda").value(false))
                .andExpect(jsonPath("$.cancelouAposSaida").value(false));

        assertThat(eventosPublicados()).isEmpty();
        Confirmacao confirmacao = confirmacaoDoDia();
        assertThat(confirmacao.isCancelouAposSaida()).isFalse();
        assertThat(confirmacao.getAtualizadoEm()).isEqualTo(DIA.atTime(17, 0));
    }

    @Test
    void cancelarIdaSemConfirmacaoNaoCriaRegistros() throws Exception {
        as("18:30");
        ida(false).andExpect(status().isOk());
        assertThat(viagens.findByTurmaIdAndData(turma.getId(), DIA)).isEmpty();
        assertThat(eventosPublicados()).isEmpty();
    }

    @Test
    void as1900IncluirVoltaEAceitoDepoisDaSaidaDaIda() throws Exception {
        as("19:00");
        volta(true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiVolta").value(true))
                .andExpect(jsonPath("$.statusVolta").value("EM_AULA"));
    }

    @Test
    void as2100StatusLiberadoComVoltaMarcadaEAceito() throws Exception {
        as("19:00");
        volta(true).andExpect(status().isOk());
        as("21:00");
        statusVolta("LIBERADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVolta").value("LIBERADO"));
        assertThat(confirmacaoDoDia().getAtualizadoEm()).isEqualTo(DIA.atTime(21, 0));
    }

    @Test
    void as2100StatusLiberadoSemVoltaMarcadaDevolve422() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("21:00");
        statusVolta("LIBERADO")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VOLTA_NAO_INCLUIDA"));

        Usuario semConfirmacao = vincularOutroAluno();
        statusVolta(dados.bearer(semConfirmacao), "LIBERADO")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VOLTA_NAO_INCLUIDA"));
    }

    @Test
    void as2229CancelarVoltaEAceitoEOStatusVoltaParaEmAula() throws Exception {
        as("19:00");
        volta(true).andExpect(status().isOk());
        statusVolta("LIBERADO").andExpect(status().isOk());
        as("22:29");
        volta(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiVolta").value(false))
                .andExpect(jsonPath("$.statusVolta").value("EM_AULA"));
    }

    @Test
    void as2230QualquerAlteracaoDevolve422DiaEncerrado() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        volta(true).andExpect(status().isOk());
        as("22:30");

        for (ResultActions resultado : List.of(ida(false), ida(true), volta(false), volta(true),
                statusVolta("LIBERADO"))) {
            resultado.andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo").value("DIA_ENCERRADO"));
        }
        Confirmacao confirmacao = confirmacaoDoDia();
        assertThat(confirmacao.isIncluiIda()).isTrue();
        assertThat(confirmacao.isIncluiVolta()).isTrue();
        assertThat(eventosPublicados()).isEmpty();
    }

    @Test
    void alunoSemVinculoAtivoComATurmaRecebe403() throws Exception {
        as("17:00");
        String deFora = dados.bearer(dados.aluno());
        Usuario removido = vincularOutroAluno();
        TurmaAluno vinculoRemovido = vinculos.findVinculoAtivo(removido.getId(), turma.getId()).orElseThrow();
        vinculoRemovido.setAtivo(false);
        vinculos.save(vinculoRemovido);

        for (String outro : new String[] { deFora, dados.bearer(removido) }) {
            mvc.perform(get("/aluno/turmas/{id}/plano", turma.getId()).header(HttpHeaders.AUTHORIZATION, outro))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            marcar(outro, "ida", "{\"incluir\": true}").andExpect(status().isForbidden());
            marcar(outro, "volta", "{\"incluir\": true}").andExpect(status().isForbidden());
            marcar(outro, "status-volta", "{\"status\": \"LIBERADO\"}").andExpect(status().isForbidden());
        }
        assertThat(viagens.findByTurmaIdAndData(turma.getId(), DIA)).isEmpty();
    }

    @Test
    void turmaInexistenteDevolve404EMotoristaNaoUsaAsRotasDoAluno() throws Exception {
        mvc.perform(get("/aluno/turmas/{id}/plano", 999_999_999L).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(put("/aluno/turmas/{id}/plano/ida", 999_999_999L).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"incluir\": true}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(turma.getMotorista())))
                .andExpect(status().isForbidden());
    }

    @Test
    void corpoInvalidoDevolve400() throws Exception {
        marcar(bearer, "ida", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.incluir").exists());
        marcar(bearer, "status-volta", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.status").exists());
        marcar(bearer, "status-volta", "{\"status\": \"ATRASADO\"}").andExpect(status().isBadRequest());
    }

    // --- Demais critérios ---

    @Test
    void getDoPlanoNaoCriaViagemNemConfirmacao() throws Exception {
        as("17:00");
        plano()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiIda").value(false))
                .andExpect(jsonPath("$.incluiVolta").value(false))
                .andExpect(jsonPath("$.statusVolta").value("EM_AULA"));
        assertThat(viagens.findByTurmaIdAndData(turma.getId(), DIA)).isEmpty();
        assertThat(confirmacoes.findDoDia(vinculo().getId(), DIA)).isEmpty();
    }

    @Test
    void marcarOQueJaEstaMarcadoDevolve200SemAlteracao() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        volta(true).andExpect(status().isOk());
        as("19:00");
        statusVolta("LIBERADO").andExpect(status().isOk());

        as("20:00");
        ida(true).andExpect(status().isOk()).andExpect(jsonPath("$.incluiIda").value(true));
        volta(true).andExpect(status().isOk()).andExpect(jsonPath("$.statusVolta").value("LIBERADO"));
        statusVolta("LIBERADO").andExpect(status().isOk());

        Confirmacao confirmacao = confirmacaoDoDia();
        assertThat(confirmacao.getAtualizadoEm()).isEqualTo(DIA.atTime(19, 0));
        assertThat(confirmacao.getStatusVolta()).isEqualTo(StatusVolta.LIBERADO);
    }

    @Test
    void acoesNoEstadoB1SemPlano() throws Exception {
        as("17:00");
        acoes(plano(), false, false, false, false, false, true, false, false, false);
    }

    @Test
    void acoesNoEstadoB2PlanoDeclarado() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        acoes(volta(true), true, true, false, false, false, false, true, false, true);
    }

    @Test
    void acoesNoEstadoB3DepoisDaSaidaComIdaMarcadaIndicaCancelamentoTardio() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("19:00");
        // B4: o app pede confirmação porque o cancelamento agora será tardio.
        acoes(plano(), true, false, false, true, false, false, true, true, false);
    }

    @Test
    void acoesNoEstadoB5IdaCanceladaDepoisDaSaida() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        as("19:00");
        acoes(ida(false), false, false, true, true, false, false, false, false, false);
    }

    @Test
    void acoesNoEstadoB6IdaBloqueada() throws Exception {
        as("19:00");
        acoes(plano(), false, false, false, true, false, false, false, false, false);
    }

    @Test
    void acoesNoEstadoB7DiaEncerrado() throws Exception {
        as("17:00");
        ida(true).andExpect(status().isOk());
        volta(true).andExpect(status().isOk());
        as("22:30");
        acoes(plano(), true, true, false, true, true, false, false, false, false);
    }

    @Test
    void getAlunoTurmasDevolveListaVaziaParaAlunoSemVinculo() throws Exception {
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAlunoTurmasTrazTurmaMotoristaEResumoDoPlanoDeHoje() throws Exception {
        Grupo manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 20), LocalTime.of(12, 10)));
        Turma daManha = turmas.save(new Turma(manha, dados.motorista("Vanderlei"), "Van 1", 1, 15));
        vinculos.save(new TurmaAluno(daManha, aluno));
        as("17:00");
        volta(true).andExpect(status().isOk());

        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].turma.id").value(daManha.getId()))
                .andExpect(jsonPath("$[0].turma.grupo.nome").value("Manhã"))
                .andExpect(jsonPath("$[0].motorista.nome").value("Vanderlei"))
                .andExpect(jsonPath("$[0].incluiIda").value(false))
                .andExpect(jsonPath("$[0].incluiVolta").value(false))
                .andExpect(jsonPath("$[1].turma.id").value(turma.getId()))
                .andExpect(jsonPath("$[1].turma.nome").value("Van 2"))
                .andExpect(jsonPath("$[1].turma.grupo.horarioSaidaIda").value("18:10"))
                .andExpect(jsonPath("$[1].motorista.nome").value("Carlos"))
                .andExpect(jsonPath("$[1].incluiIda").value(false))
                .andExpect(jsonPath("$[1].incluiVolta").value(true));

        // No dia seguinte o resumo recomeça desmarcado.
        relogio.fixar(DIA.plusDays(1).atTime(16, 0));
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[1].incluiVolta").value(false));
    }

    @Test
    void primeirasConfirmacoesSimultaneasCriamUmaSoViagem() throws Exception {
        as("17:00");
        List<UsuarioAutenticado> alunos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            alunos.add(UsuarioAutenticado.de(vincularOutroAluno()));
        }
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(alunos.size());
        try {
            List<Future<PlanoResposta>> resultados = new ArrayList<>();
            for (UsuarioAutenticado outro : alunos) {
                Callable<PlanoResposta> tarefa = () -> {
                    largada.await();
                    return service.alterarIda(outro, turma.getId(), true);
                };
                resultados.add(executor.submit(tarefa));
            }
            largada.countDown();
            for (Future<PlanoResposta> resultado : resultados) {
                assertThat(resultado.get().incluiIda()).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(viagens.findByTurmaIdAndData(turma.getId(), DIA)).isPresent();
        for (UsuarioAutenticado outro : alunos) {
            TurmaAluno vinculo = vinculos.findVinculoAtivo(outro.id(), turma.getId()).orElseThrow();
            assertThat(confirmacoes.findDoDia(vinculo.getId(), DIA)).isPresent();
        }
    }

    private void as(String horario) {
        relogio.fixar(LocalDateTime.of(DIA, LocalTime.parse(horario)));
    }

    private Usuario vincularOutroAluno() {
        Usuario outro = dados.aluno();
        vinculos.save(new TurmaAluno(turma, outro));
        return outro;
    }

    private TurmaAluno vinculo() {
        return vinculos.findVinculoAtivo(aluno.getId(), turma.getId()).orElseThrow();
    }

    private Confirmacao confirmacaoDoDia() {
        return confirmacoes.findDoDia(vinculo().getId(), DIA).orElseThrow();
    }

    private List<IdaCanceladaAposSaida> eventosPublicados() {
        return eventos.stream(IdaCanceladaAposSaida.class).toList();
    }

    private ResultActions plano() throws Exception {
        return mvc.perform(get("/aluno/turmas/{id}/plano", turma.getId()).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions ida(boolean incluir) throws Exception {
        return marcar(bearer, "ida", "{\"incluir\": %s}".formatted(incluir));
    }

    private ResultActions volta(boolean incluir) throws Exception {
        return marcar(bearer, "volta", "{\"incluir\": %s}".formatted(incluir));
    }

    private ResultActions statusVolta(String status) throws Exception {
        return statusVolta(bearer, status);
    }

    private ResultActions statusVolta(String quem, String status) throws Exception {
        return marcar(quem, "status-volta", "{\"status\": \"%s\"}".formatted(status));
    }

    private ResultActions marcar(String quem, String acao, String corpo) throws Exception {
        return mvc.perform(put("/aluno/turmas/{id}/plano/{acao}", turma.getId(), acao)
                .header(HttpHeaders.AUTHORIZATION, quem)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /** Confere o estado do plano e o bloco {@code acoes}, na ordem dos campos da resposta. */
    private static void acoes(ResultActions resultado, boolean incluiIda, boolean incluiVolta,
            boolean cancelouAposSaida, boolean idaSaiu, boolean voltaSaiu, boolean podeIncluirIda,
            boolean podeCancelarIda, boolean cancelamentoIdaSeraTardio, boolean podeAlterarStatusVolta)
            throws Exception {
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.incluiIda").value(incluiIda))
                .andExpect(jsonPath("$.incluiVolta").value(incluiVolta))
                .andExpect(jsonPath("$.cancelouAposSaida").value(cancelouAposSaida))
                .andExpect(jsonPath("$.idaSaiu").value(idaSaiu))
                .andExpect(jsonPath("$.voltaSaiu").value(voltaSaiu))
                .andExpect(jsonPath("$.acoes.podeIncluirIda").value(podeIncluirIda))
                .andExpect(jsonPath("$.acoes.podeCancelarIda").value(podeCancelarIda))
                .andExpect(jsonPath("$.acoes.cancelamentoIdaSeraTardio").value(cancelamentoIdaSeraTardio))
                .andExpect(jsonPath("$.acoes.podeAlterarVolta").value(!voltaSaiu))
                .andExpect(jsonPath("$.acoes.podeAlterarStatusVolta").value(podeAlterarStatusVolta));
    }
}
