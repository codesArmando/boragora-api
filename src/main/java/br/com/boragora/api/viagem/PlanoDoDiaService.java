package br.com.boragora.api.viagem;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.seguranca.ControleDeAcesso;
import br.com.boragora.api.seguranca.UsuarioAutenticado;
import br.com.boragora.api.turma.PessoaDaTurma;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaDoAluno;
import br.com.boragora.api.turma.TurmaRepository;

/**
 * Plano do dia do aluno (RN01 a RN07, RN14 e RN15). "Hoje" e "agora" vêm sempre do {@link Clock}
 * injetado. Uma ação "até a saída" é aceita enquanto {@code agora < horário}.
 */
@Service
public class PlanoDoDiaService {

    private static final Comparator<TurmaAluno> ORDEM_DAS_TURMAS = Comparator
            .comparing((TurmaAluno vinculo) -> vinculo.getTurma().getGrupo(), Grupo.ORDEM_DE_EXIBICAO)
            .thenComparing(vinculo -> vinculo.getTurma().getNome(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(vinculo -> vinculo.getTurma().getId());

    private final TurmaAlunoRepository vinculos;
    private final TurmaRepository turmas;
    private final ViagemRepository viagens;
    private final ConfirmacaoRepository confirmacoes;
    private final CriadorDeViagem criadorDeViagem;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public PlanoDoDiaService(TurmaAlunoRepository vinculos, TurmaRepository turmas, ViagemRepository viagens,
            ConfirmacaoRepository confirmacoes, CriadorDeViagem criadorDeViagem, ApplicationEventPublisher eventos,
            Clock relogio) {
        this.vinculos = vinculos;
        this.turmas = turmas;
        this.viagens = viagens;
        this.confirmacoes = confirmacoes;
        this.criadorDeViagem = criadorDeViagem;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** Turmas com vínculo ativo e o resumo do plano de hoje. Lista vazia é a tela A3. */
    @Transactional(readOnly = true)
    public List<TurmaDoAlunoResposta> listarTurmas(UsuarioAutenticado aluno) {
        List<TurmaAluno> ativos = vinculos.findAtivosDoAlunoComTurma(aluno.id());
        if (ativos.isEmpty()) {
            return List.of();
        }
        Map<Long, Confirmacao> doDia = confirmacoes
                .findDoDia(ativos.stream().map(TurmaAluno::getId).toList(), LocalDate.now(relogio)).stream()
                .collect(Collectors.toMap(confirmacao -> confirmacao.getTurmaAluno().getId(), Function.identity()));
        return ativos.stream()
                .sorted(ORDEM_DAS_TURMAS)
                .map(vinculo -> {
                    Optional<Confirmacao> confirmacao = Optional.ofNullable(doDia.get(vinculo.getId()));
                    Turma turma = vinculo.getTurma();
                    return new TurmaDoAlunoResposta(TurmaDoAluno.de(turma), PessoaDaTurma.de(turma.getMotorista()),
                            confirmacao.map(Confirmacao::isIncluiIda).orElse(false),
                            confirmacao.map(Confirmacao::isIncluiVolta).orElse(false));
                })
                .toList();
    }

    /** Nunca cria registros: sem confirmação, o plano vem todo desmarcado (RN04). */
    @Transactional(readOnly = true)
    public PlanoResposta planoDeHoje(UsuarioAutenticado aluno, Long turmaId) {
        TurmaAluno vinculo = exigirVinculo(vinculos.findVinculoAtivo(aluno.id(), turmaId), turmaId);
        Momento momento = Momento.de(relogio, vinculo.getTurma().getGrupo());
        return montar(vinculo, confirmacoes.findDoDia(vinculo.getId(), momento.hoje()).orElse(null), momento);
    }

    /** RN02 (incluir só antes da saída) e RN05 (cancelar depois da saída avisa o motorista). */
    @Transactional
    public PlanoResposta alterarIda(UsuarioAutenticado aluno, Long turmaId, boolean incluir) {
        TurmaAluno vinculo = travarVinculo(aluno, turmaId);
        Momento momento = momentoComDiaAberto(vinculo);
        Confirmacao atual = confirmacoes.findDoDia(vinculo.getId(), momento.hoje()).orElse(null);
        if ((atual != null && atual.isIncluiIda()) == incluir) {
            return montar(vinculo, atual, momento);
        }
        if (incluir && momento.idaSaiu()) {
            throw new RegraDeNegocioException("IDA_ENCERRADA", HttpStatus.UNPROCESSABLE_CONTENT,
                    "Não dá mais para incluir a ida pelo app. Fale com o motorista pelo WhatsApp.");
        }
        Confirmacao confirmacao = atual != null ? atual : nova(vinculo, momento);
        confirmacao.setIncluiIda(incluir);
        boolean tardio = !incluir && momento.idaSaiu();
        if (tardio) {
            confirmacao.setCancelouAposSaida(true);
        }
        gravar(confirmacao, momento);
        if (tardio) {
            eventos.publishEvent(new IdaCanceladaAposSaida(turmaId, aluno.id(), confirmacao.getId(), momento.hoje()));
        }
        return montar(vinculo, confirmacao, momento);
    }

    /** RN03 e RN14: a volta pode ser incluída ou cancelada até a saída da volta; o status recomeça em EM_AULA. */
    @Transactional
    public PlanoResposta alterarVolta(UsuarioAutenticado aluno, Long turmaId, boolean incluir) {
        TurmaAluno vinculo = travarVinculo(aluno, turmaId);
        Momento momento = momentoComDiaAberto(vinculo);
        Confirmacao atual = confirmacoes.findDoDia(vinculo.getId(), momento.hoje()).orElse(null);
        if ((atual != null && atual.isIncluiVolta()) == incluir) {
            return montar(vinculo, atual, momento);
        }
        Confirmacao confirmacao = atual != null ? atual : nova(vinculo, momento);
        confirmacao.setIncluiVolta(incluir);
        confirmacao.setStatusVolta(StatusVolta.EM_AULA);
        gravar(confirmacao, momento);
        return montar(vinculo, confirmacao, momento);
    }

    /** RN07: o status só vale para quem tem a volta no plano. */
    @Transactional
    public PlanoResposta alterarStatusVolta(UsuarioAutenticado aluno, Long turmaId, StatusVolta status) {
        TurmaAluno vinculo = travarVinculo(aluno, turmaId);
        Momento momento = momentoComDiaAberto(vinculo);
        Confirmacao atual = confirmacoes.findDoDia(vinculo.getId(), momento.hoje())
                .filter(Confirmacao::isIncluiVolta)
                .orElseThrow(() -> new RegraDeNegocioException("VOLTA_NAO_INCLUIDA", HttpStatus.UNPROCESSABLE_CONTENT,
                        "Inclua a volta no plano de hoje antes de informar o status."));
        if (atual.getStatusVolta() != status) {
            atual.setStatusVolta(status);
            gravar(atual, momento);
        }
        return montar(vinculo, atual, momento);
    }

    private TurmaAluno travarVinculo(UsuarioAutenticado aluno, Long turmaId) {
        return exigirVinculo(vinculos.travarVinculoAtivo(aluno.id(), turmaId), turmaId);
    }

    /** Turma inexistente é 404; turma em que o aluno não tem vínculo ativo é 403. */
    private TurmaAluno exigirVinculo(Optional<TurmaAluno> vinculo, Long turmaId) {
        return vinculo.orElseThrow(() -> turmas.existsById(turmaId)
                ? ControleDeAcesso.acessoNegado()
                : RegraDeNegocioException.naoEncontrado("Turma não encontrada."));
    }

    /** RN15: a partir da saída da volta nada mais muda no dia, nem mesmo o cancelamento da ida. */
    private Momento momentoComDiaAberto(TurmaAluno vinculo) {
        Momento momento = Momento.de(relogio, vinculo.getTurma().getGrupo());
        if (momento.voltaSaiu()) {
            throw new RegraDeNegocioException("DIA_ENCERRADO", HttpStatus.UNPROCESSABLE_CONTENT,
                    "A volta de hoje já saiu. O plano não pode mais ser alterado.");
        }
        return momento;
    }

    private Confirmacao nova(TurmaAluno vinculo, Momento momento) {
        return new Confirmacao(viagemDoDia(vinculo.getTurma().getId(), momento.hoje()), vinculo, momento.agora());
    }

    /** A viagem é criada na primeira confirmação do dia; se outro aluno a criar junto, usa a dele. */
    private Viagem viagemDoDia(Long turmaId, LocalDate hoje) {
        return viagens.findByTurmaIdAndData(turmaId, hoje).orElseGet(() -> {
            try {
                criadorDeViagem.criar(turmaId, hoje);
            } catch (DataIntegrityViolationException e) {
                // Outra transação criou a viagem entre a busca e a gravação.
            }
            return viagens.lerAtualPorTurmaEData(turmaId, hoje).orElseThrow();
        });
    }

    private void gravar(Confirmacao confirmacao, Momento momento) {
        confirmacao.setAtualizadoEm(momento.agora());
        confirmacoes.saveAndFlush(confirmacao);
    }

    private static PlanoResposta montar(TurmaAluno vinculo, Confirmacao confirmacao, Momento momento) {
        boolean ida = confirmacao != null && confirmacao.isIncluiIda();
        boolean volta = confirmacao != null && confirmacao.isIncluiVolta();
        boolean idaSaiu = momento.idaSaiu();
        boolean voltaSaiu = momento.voltaSaiu();
        AcoesDoPlano acoes = new AcoesDoPlano(
                !ida && !idaSaiu,
                ida && !voltaSaiu,
                ida && idaSaiu && !voltaSaiu,
                !voltaSaiu,
                volta && !voltaSaiu);
        return new PlanoResposta(momento.hoje(), TurmaDoAluno.de(vinculo.getTurma()), ida, volta,
                confirmacao == null ? StatusVolta.EM_AULA : confirmacao.getStatusVolta(),
                confirmacao != null && confirmacao.isCancelouAposSaida(), idaSaiu, voltaSaiu, acoes);
    }

    /** O instante da requisição, lido uma vez só, e a posição dele em relação aos horários do grupo. */
    private record Momento(LocalDateTime agora, boolean idaSaiu, boolean voltaSaiu) {

        static Momento de(Clock relogio, Grupo grupo) {
            LocalDateTime agora = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
            LocalTime hora = agora.toLocalTime();
            return new Momento(agora, !hora.isBefore(grupo.getHorarioSaidaIda()),
                    !hora.isBefore(grupo.getHorarioSaidaVolta()));
        }

        LocalDate hoje() {
            return agora.toLocalDate();
        }
    }
}
