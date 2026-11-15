package br.com.boragora.api.viagem;

import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.GrupoResposta.MinhaTurma;
import br.com.boragora.api.seguranca.ControleDeAcesso;
import br.com.boragora.api.seguranca.UsuarioAutenticado;
import br.com.boragora.api.turma.GrupoDaTurma;
import br.com.boragora.api.turma.PessoaDaTurma;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.turma.TurmaService;
import br.com.boragora.api.usuario.Papel;

/**
 * Listas e contadores do dia (RF04, RF05, RF06, RF12 e RF16), com a visibilidade do
 * {@link ControleDeAcesso}. Só lê: nenhuma consulta cria viagem. As confirmações de todas as
 * turmas pedidas vêm em uma única consulta.
 */
@Service
public class ListagemDoDiaService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Comparator<Turma> ORDEM_NO_GRUPO = Comparator
            .comparing(Turma::getNome, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Turma::getId);

    private final TurmaRepository turmas;
    private final GrupoRepository grupos;
    private final TurmaAlunoRepository vinculos;
    private final ConfirmacaoRepository confirmacoes;
    private final ControleDeAcesso controleDeAcesso;
    private final Clock relogio;

    public ListagemDoDiaService(TurmaRepository turmas, GrupoRepository grupos, TurmaAlunoRepository vinculos,
            ConfirmacaoRepository confirmacoes, ControleDeAcesso controleDeAcesso, Clock relogio) {
        this.turmas = turmas;
        this.grupos = grupos;
        this.vinculos = vinculos;
        this.confirmacoes = confirmacoes;
        this.controleDeAcesso = controleDeAcesso;
        this.relogio = relogio;
    }

    /** Turmas que o motorista conduz (telas C0 e C6). Lista vazia para o chefe que não conduz turma (C0b). */
    @Transactional(readOnly = true)
    public List<TurmaResumoHoje> listarDoMotorista(UsuarioAutenticado motorista) {
        List<Turma> lista = turmas.findDoMotoristaComGrupoEMotorista(motorista.id()).stream()
                .sorted(TurmaService.ORDEM_CADASTRAL)
                .toList();
        Map<Long, DiaDaTurma> dias = calcular(lista, motorista.id());
        return lista.stream().map(turma -> {
            DiaDaTurma dia = dias.get(turma.getId());
            return new TurmaResumoHoje(turma.getId(), turma.getNome(), GrupoDaTurma.de(turma.getGrupo()),
                    PessoaDaTurma.de(turma.getMotorista()), dia.contadores(), dia.semConfirmacoes());
        }).toList();
    }

    @Transactional(readOnly = true)
    public TurmaHojeResposta hojeDaTurma(Long id, UsuarioAutenticado usuario) {
        Turma turma = turmas.findComGrupoEMotorista(id)
                .orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Turma não encontrada."));
        controleDeAcesso.exigirAcessoATurma(usuario, id);
        DiaDaTurma dia = calcular(List.of(turma), usuario.id()).get(id);
        return new TurmaHojeResposta(turma.getId(), turma.getNome(), GrupoDaTurma.de(turma.getGrupo()),
                PessoaDaTurma.de(turma.getMotorista()), dia.contadores(), dia.semConfirmacoes(), dia.listaIda(),
                dia.listaVolta());
    }

    /** Todas as turmas do grupo, em ordem de nome, com a turma de quem chama marcada. */
    @Transactional(readOnly = true)
    public GrupoHojeResposta hojeDoGrupo(Long id, UsuarioAutenticado usuario) {
        Grupo grupo = grupos.findById(id)
                .orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Grupo não encontrado."));
        controleDeAcesso.exigirAcessoAoGrupo(usuario, id);
        List<Turma> doGrupo = turmas.findDoGrupoComGrupoEMotorista(id).stream().sorted(ORDEM_NO_GRUPO).toList();
        Set<Long> minhas = minhasTurmas(usuario, doGrupo);
        Map<Long, DiaDaTurma> dias = calcular(doGrupo, usuario.id());

        List<GrupoHojeResposta.TurmaDoGrupoHoje> itens = doGrupo.stream().map(turma -> {
            DiaDaTurma dia = dias.get(turma.getId());
            return new GrupoHojeResposta.TurmaDoGrupoHoje(turma.getId(), turma.getNome(),
                    PessoaDaTurma.de(turma.getMotorista()), minhas.contains(turma.getId()), dia.contadores(),
                    dia.semConfirmacoes(), dia.listaIda(), dia.listaVolta());
        }).toList();
        MinhaTurma minhaTurma = doGrupo.stream()
                .filter(turma -> minhas.contains(turma.getId()))
                .findFirst()
                .map(turma -> new MinhaTurma(turma.getId(), turma.getNome()))
                .orElse(null);
        return new GrupoHojeResposta(grupo.getId(), grupo.getNome(), grupo.getPeriodo(), grupo.getHorarioSaidaIda(),
                grupo.getHorarioSaidaVolta(), minhaTurma, itens);
    }

    /** Motorista: as turmas que conduz. Aluno: as turmas em que tem vínculo ativo. */
    private Set<Long> minhasTurmas(UsuarioAutenticado usuario, List<Turma> doGrupo) {
        if (usuario.papel() == Papel.MOTORISTA) {
            return doGrupo.stream()
                    .filter(turma -> turma.getMotorista().getId().equals(usuario.id()))
                    .map(Turma::getId)
                    .collect(Collectors.toSet());
        }
        return vinculos.findTurmasAtivasDoAluno(usuario.id()).stream().map(Turma::getId).collect(Collectors.toSet());
    }

    /** O dia de cada turma da lista, com uma consulta só para todas. */
    private Map<Long, DiaDaTurma> calcular(List<Turma> lista, Long usuarioId) {
        if (lista.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Confirmacao>> porTurma = confirmacoes
                .findDasListasDoDia(lista.stream().map(Turma::getId).toList(), LocalDate.now(relogio)).stream()
                .collect(Collectors.groupingBy(confirmacao -> confirmacao.getViagem().getTurma().getId()));
        Comparator<String> alfabetica = Collator.getInstance(PT_BR)::compare;
        return lista.stream().collect(Collectors.toMap(Turma::getId, turma -> DiaDaTurma
                .de(porTurma.getOrDefault(turma.getId(), List.of()), usuarioId, alfabetica), (a, b) -> a));
    }

    private record DiaDaTurma(ContadoresDoDia contadores, boolean semConfirmacoes, List<ItemDaIda> listaIda,
            List<ItemDaVolta> listaVolta) {

        static DiaDaTurma de(List<Confirmacao> doDia, Long usuarioId, Comparator<String> alfabetica) {
            List<ItemDaIda> ida = doDia.stream()
                    .filter(confirmacao -> confirmacao.isIncluiIda() || confirmacao.isCancelouAposSaida())
                    .map(confirmacao -> {
                        var aluno = confirmacao.getTurmaAluno().getAluno();
                        return new ItemDaIda(aluno.getId(), aluno.getNomePessoa(),
                                confirmacao.isIncluiIda() ? SituacaoIda.CONFIRMADO : SituacaoIda.CANCELADO_APOS_SAIDA,
                                aluno.getId().equals(usuarioId));
                    })
                    .sorted(Comparator
                            .comparing((ItemDaIda item) -> item.situacao() != SituacaoIda.CANCELADO_APOS_SAIDA)
                            .thenComparing(ItemDaIda::nome, alfabetica)
                            .thenComparing(ItemDaIda::alunoId))
                    .toList();
            List<ItemDaVolta> volta = doDia.stream()
                    .filter(Confirmacao::isIncluiVolta)
                    .map(confirmacao -> {
                        var aluno = confirmacao.getTurmaAluno().getAluno();
                        return new ItemDaVolta(aluno.getId(), aluno.getNomePessoa(), confirmacao.getStatusVolta(),
                                aluno.getId().equals(usuarioId));
                    })
                    .sorted(Comparator
                            .comparing((ItemDaVolta item) -> item.statusVolta() != StatusVolta.LIBERADO)
                            .thenComparing(ItemDaVolta::nome, alfabetica)
                            .thenComparing(ItemDaVolta::alunoId))
                    .toList();
            long cancelados = ida.stream().filter(item -> item.situacao() == SituacaoIda.CANCELADO_APOS_SAIDA).count();
            long liberados = volta.stream().filter(item -> item.statusVolta() == StatusVolta.LIBERADO).count();
            ContadoresDoDia contadores = new ContadoresDoDia(ida.size() - cancelados, volta.size(), liberados,
                    cancelados);
            return new DiaDaTurma(contadores, ida.isEmpty() && volta.isEmpty(), ida, volta);
        }
    }
}
