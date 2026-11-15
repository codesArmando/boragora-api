package br.com.boragora.api.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.PushGatewayFalso;
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
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.viagem.ConfirmacaoRepository;
import br.com.boragora.api.viagem.PlanoDoDiaService;

/**
 * Push de cancelamento tardio e lembrete do plano. Grupo "Noite" com ida às 18:10 e volta às 22:30;
 * {@link #SEGUNDA} é uma segunda-feira. Os envios são conferidos pelo token de cada destinatário,
 * porque outros testes também podem gerar envios no mesmo gateway falso.
 */
@TesteDeIntegracao
class NotificacaoApiTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 5);
    private static final LocalDate SABADO = LocalDate.of(2026, 10, 10);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private RelogioDeTeste relogio;
    @Autowired
    private PushGatewayFalso gateway;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ConfirmacaoRepository confirmacoes;
    @Autowired
    private PlanoDoDiaService planos;
    @Autowired
    private LembreteDoPlano lembrete;

    private Grupo grupo;
    private Turma turma;
    private Usuario carlos;

    @BeforeEach
    void montarTurma() {
        relogio.fixar(SEGUNDA.atTime(15, 0));
        grupo = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
        carlos = comToken(dados.motorista("Carlos"));
        turma = turmas.save(new Turma(grupo, carlos, "Van 2", 2, 15));
    }

    @AfterEach
    void limpar() {
        relogio.voltarAoReal();
        gateway.limpar();
    }

    // --- Cancelamento tardio ---

    @Test
    void cancelarIdaDepoisDaSaidaEnviaUmPushSoAoMotoristaDaTurma() {
        Usuario chefe = dados.admin();
        chefe = comToken(chefe);
        Usuario joao = aluno("João Gabriel");
        as(SEGUNDA, "17:00");
        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), true);
        as(SEGUNDA, "18:30");

        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), false);

        List<MensagemPush> paraCarlos = gateway.enviadasPara(carlos.getExpoPushToken());
        assertThat(paraCarlos).hasSize(1);
        MensagemPush push = paraCarlos.getFirst();
        assertThat(push.titulo()).isEqualTo("João Gabriel cancelou a ida para a faculdade!");
        assertThat(push.corpo()).isEqualTo("Van 2 · Noite · depois da saída das 18:10");
        assertThat(push.data()).isEqualTo(Map.of("tipo", "CANCELAMENTO_TARDIO", "turmaId", turma.getId()));
        // O motorista-chefe não é o responsável pela turma.
        assertThat(gateway.enviadasPara(chefe.getExpoPushToken())).isEmpty();
        // O aluno também não recebe nada.
        assertThat(gateway.enviadasPara(joao.getExpoPushToken())).isEmpty();
    }

    @Test
    void cancelarIdaAntesDaSaidaOuIdaNaoMarcadaNaoEnviaPush() {
        Usuario joao = aluno("João");
        Usuario semIda = aluno("Sem ida");
        as(SEGUNDA, "17:00");
        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), true);
        as(SEGUNDA, "17:30");
        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), false);
        as(SEGUNDA, "18:30");
        planos.alterarIda(UsuarioAutenticado.de(semIda), turma.getId(), false);

        assertThat(gateway.enviadasPara(carlos.getExpoPushToken())).isEmpty();
    }

    @Test
    void seOGatewayFalharOCancelamentoContinuaGravado() throws Exception {
        Usuario joao = aluno("João");
        as(SEGUNDA, "17:00");
        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), true);
        as(SEGUNDA, "18:30");
        gateway.falharCom(new IllegalStateException("Expo fora do ar"));

        mvc.perform(put("/aluno/turmas/{id}/plano/ida", turma.getId())
                .header(HttpHeaders.AUTHORIZATION, dados.bearer(joao))
                .contentType(MediaType.APPLICATION_JSON).content("{\"incluir\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelouAposSaida").value(true));

        assertThat(gateway.enviadasPara(carlos.getExpoPushToken())).hasSize(1);
        TurmaAluno vinculo = vinculos.findVinculoAtivo(joao.getId(), turma.getId()).orElseThrow();
        var confirmacao = confirmacoes.findDoDia(vinculo.getId(), SEGUNDA).orElseThrow();
        assertThat(confirmacao.isIncluiIda()).isFalse();
        assertThat(confirmacao.isCancelouAposSaida()).isTrue();
    }

    @Test
    void motoristaSemTokenEIgnoradoSemErro() {
        Usuario semToken = dados.motorista();
        Turma outra = turmas.save(new Turma(grupo, semToken, "Van 3", 3, 15));
        Usuario joao = dados.aluno("João");
        vinculos.save(new TurmaAluno(outra, joao));
        as(SEGUNDA, "17:00");
        planos.alterarIda(UsuarioAutenticado.de(joao), outra.getId(), true);
        as(SEGUNDA, "18:30");

        var plano = planos.alterarIda(UsuarioAutenticado.de(joao), outra.getId(), false);

        assertThat(plano.cancelouAposSaida()).isTrue();
    }

    @Test
    void tokenRecusadoPeloExpoComoNaoRegistradoEApagado() {
        Usuario joao = aluno("João");
        as(SEGUNDA, "17:00");
        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), true);
        as(SEGUNDA, "18:30");
        gateway.naoRegistrado(carlos.getExpoPushToken());

        planos.alterarIda(UsuarioAutenticado.de(joao), turma.getId(), false);

        assertThat(gateway.enviadasPara(carlos.getExpoPushToken())).hasSize(1);
        assertThat(usuarios.findById(carlos.getId()).orElseThrow().getExpoPushToken()).isNull();
    }

    // --- Lembrete do plano do dia ---

    @Test
    void as1610OLembreteVaiSoParaQuemNaoMarcouNada() {
        Usuario semPlano = aluno("Sem plano");
        Usuario comIda = aluno("Com ida");
        Usuario comVolta = aluno("Com volta");
        Usuario cancelouAntes = aluno("Cancelou antes");
        Usuario semToken = dados.aluno("Sem token");
        vinculos.save(new TurmaAluno(turma, semToken));
        Usuario removido = aluno("Removido");
        TurmaAluno vinculoRemovido = vinculos.findVinculoAtivo(removido.getId(), turma.getId()).orElseThrow();
        vinculoRemovido.setAtivo(false);
        vinculos.save(vinculoRemovido);
        as(SEGUNDA, "15:00");
        planos.alterarIda(UsuarioAutenticado.de(comIda), turma.getId(), true);
        planos.alterarVolta(UsuarioAutenticado.de(comVolta), turma.getId(), true);
        planos.alterarIda(UsuarioAutenticado.de(cancelouAntes), turma.getId(), true);
        planos.alterarIda(UsuarioAutenticado.de(cancelouAntes), turma.getId(), false);

        as(SEGUNDA, "16:10");
        lembrete.enviar();

        List<MensagemPush> recebidas = gateway.enviadasPara(semPlano.getExpoPushToken());
        assertThat(recebidas).hasSize(1);
        MensagemPush push = recebidas.getFirst();
        assertThat(push.titulo()).isEqualTo("Vai de van hoje?");
        assertThat(push.corpo()).isEqualTo("Marque ida e volta até 18:10 para entrar na lista do motorista.");
        assertThat(push.data()).isEqualTo(Map.of("tipo", "LEMBRETE", "turmaId", turma.getId()));
        // Quem cancelou antes da saída está com tudo desmarcado: é como não ter declarado (RN04).
        assertThat(gateway.enviadasPara(cancelouAntes.getExpoPushToken())).hasSize(1);
        assertThat(gateway.enviadasPara(comIda.getExpoPushToken())).isEmpty();
        assertThat(gateway.enviadasPara(comVolta.getExpoPushToken())).isEmpty();
        assertThat(gateway.enviadasPara(removido.getExpoPushToken())).isEmpty();
        assertThat(gateway.enviadasPara(carlos.getExpoPushToken())).isEmpty();
    }

    @Test
    void oLembreteDeCadaAlunoLevaATurmaDeleNoGrupo() {
        Turma van3 = turmas.save(new Turma(grupo, comToken(dados.motorista()), "Van 3", 3, 15));
        Usuario daVan2 = aluno("Da Van 2");
        Usuario daVan3 = comToken(dados.aluno("Da Van 3"));
        vinculos.save(new TurmaAluno(van3, daVan3));

        as(SEGUNDA, "16:10");
        lembrete.enviar();

        assertThat(gateway.enviadasPara(daVan2.getExpoPushToken()).getFirst().data().get("turmaId"))
                .isEqualTo(turma.getId());
        assertThat(gateway.enviadasPara(daVan3.getExpoPushToken()).getFirst().data().get("turmaId"))
                .isEqualTo(van3.getId());
    }

    @Test
    void umMinutoAntesOuDepoisDoDisparoNadaEEnviado() {
        Usuario semPlano = aluno("Sem plano");
        for (String horario : new String[] { "16:09", "16:11" }) {
            as(SEGUNDA, horario);
            lembrete.enviar();
        }
        assertThat(gateway.enviadasPara(semPlano.getExpoPushToken())).isEmpty();
    }

    @Test
    void foraDosDiasConfiguradosNadaEEnviado() {
        Usuario semPlano = aluno("Sem plano");
        as(SABADO, "16:10");
        lembrete.enviar();
        as(SABADO.plusDays(1), "16:10");
        lembrete.enviar();
        assertThat(gateway.enviadasPara(semPlano.getExpoPushToken())).isEmpty();
    }

    @Test
    void falhaNoEnvioDeUmLembreteNaoImpedeAsDemais() {
        Usuario primeiro = aluno("Primeiro");
        Usuario segundo = aluno("Segundo");
        gateway.falharCom(new IllegalStateException("Expo fora do ar"));

        as(SEGUNDA, "16:10");
        lembrete.enviar();

        assertThat(gateway.enviadasPara(primeiro.getExpoPushToken())).hasSize(1);
        assertThat(gateway.enviadasPara(segundo.getExpoPushToken())).hasSize(1);
    }

    private void as(LocalDate dia, String horario) {
        relogio.fixar(LocalDateTime.of(dia, LocalTime.parse(horario)));
    }

    /** Aluno com token e vínculo ativo na turma. */
    private Usuario aluno(String nome) {
        Usuario aluno = comToken(dados.aluno(nome));
        vinculos.save(new TurmaAluno(turma, aluno));
        return aluno;
    }

    private Usuario comToken(Usuario usuario) {
        usuario.setExpoPushToken("ExponentPushToken[" + UUID.randomUUID() + "]");
        return usuarios.save(usuario);
    }
}
