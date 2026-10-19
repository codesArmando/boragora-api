package br.com.boragora.api.grupo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.grupo.GrupoResposta.MinhaTurma;
import br.com.boragora.api.seguranca.ControleDeAcesso;
import br.com.boragora.api.seguranca.UsuarioAutenticado;
import br.com.boragora.api.turma.TotalDeTurmas;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Papel;

@Service
public class GrupoService {

    private static final Comparator<Grupo> ORDEM = Comparator
            .comparing(Grupo::getPeriodo)
            .thenComparing(Grupo::getHorarioSaidaIda)
            .thenComparing(Grupo::getNome, String.CASE_INSENSITIVE_ORDER);

    private final GrupoRepository grupos;
    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;
    private final ControleDeAcesso controleDeAcesso;

    public GrupoService(GrupoRepository grupos, TurmaRepository turmas, TurmaAlunoRepository vinculos,
            ControleDeAcesso controleDeAcesso) {
        this.grupos = grupos;
        this.turmas = turmas;
        this.vinculos = vinculos;
        this.controleDeAcesso = controleDeAcesso;
    }

    @Transactional
    public GrupoResposta criar(GrupoRequisicao requisicao, UsuarioAutenticado usuario) {
        validarHorarios(requisicao);
        Grupo grupo = grupos.save(new Grupo(requisicao.nome().trim(), requisicao.periodo(),
                requisicao.horarioSaidaIda(), requisicao.horarioSaidaVolta()));
        return montarRespostas(List.of(grupo), usuario).getFirst();
    }

    /** Horários alterados valem na hora, inclusive para o dia em andamento: as regras sempre leem do grupo. */
    @Transactional
    public GrupoResposta atualizar(Long id, GrupoRequisicao requisicao, UsuarioAutenticado usuario) {
        validarHorarios(requisicao);
        Grupo grupo = buscar(id);
        grupo.setNome(requisicao.nome().trim());
        grupo.setPeriodo(requisicao.periodo());
        grupo.setHorarioSaidaIda(requisicao.horarioSaidaIda());
        grupo.setHorarioSaidaVolta(requisicao.horarioSaidaVolta());
        return montarRespostas(List.of(grupo), usuario).getFirst();
    }

    /** Só apaga grupo sem turmas. A chave estrangeira de turma.id_grupo é a segunda barreira. */
    @Transactional
    public void excluir(Long id) {
        Grupo grupo = buscar(id);
        if (turmas.existsByGrupoId(id)) {
            throw grupoComTurmas();
        }
        try {
            grupos.delete(grupo);
            grupos.flush();
        } catch (DataIntegrityViolationException e) {
            throw grupoComTurmas();
        }
    }

    @Transactional(readOnly = true)
    public List<GrupoResposta> listar(UsuarioAutenticado usuario) {
        List<Grupo> visiveis = controleDeAcesso.gruposVisiveis(usuario).stream().sorted(ORDEM).toList();
        return montarRespostas(visiveis, usuario);
    }

    @Transactional(readOnly = true)
    public GrupoResposta detalhar(Long id, UsuarioAutenticado usuario) {
        Grupo grupo = buscar(id);
        controleDeAcesso.exigirAcessoAoGrupo(usuario, id);
        return montarRespostas(List.of(grupo), usuario).getFirst();
    }

    private Grupo buscar(Long id) {
        return grupos.findById(id).orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Grupo não encontrado."));
    }

    private static void validarHorarios(GrupoRequisicao requisicao) {
        if (!requisicao.horarioSaidaIda().isBefore(requisicao.horarioSaidaVolta())) {
            throw RegraDeNegocioException.validacao("horarioSaidaVolta",
                    "O horário de saída da volta precisa ser posterior ao da ida.");
        }
    }

    private static RegraDeNegocioException grupoComTurmas() {
        return new RegraDeNegocioException("GRUPO_COM_TURMAS", HttpStatus.CONFLICT,
                "Este grupo ainda tem turmas. Exclua as turmas antes de excluir o grupo.");
    }

    /** Monta as respostas com duas consultas no total: contagem de turmas e turmas de quem chama. */
    private List<GrupoResposta> montarRespostas(List<Grupo> lista, UsuarioAutenticado usuario) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Grupo::getId).toList();
        Map<Long, Long> totais = turmas.contarPorGrupo(ids).stream()
                .collect(Collectors.toMap(TotalDeTurmas::grupoId, TotalDeTurmas::total));
        Map<Long, Turma> minhas = turmasDe(usuario).stream()
                .collect(Collectors.toMap(t -> t.getGrupo().getId(), Function.identity(), (a, b) -> a));

        return lista.stream().map(grupo -> {
            Turma minha = minhas.get(grupo.getId());
            return new GrupoResposta(grupo.getId(), grupo.getNome(), grupo.getPeriodo(),
                    grupo.getHorarioSaidaIda(), grupo.getHorarioSaidaVolta(),
                    totais.getOrDefault(grupo.getId(), 0L),
                    minha == null ? null : new MinhaTurma(minha.getId(), minha.getNome()));
        }).toList();
    }

    private List<Turma> turmasDe(UsuarioAutenticado usuario) {
        return usuario.papel() == Papel.MOTORISTA
                ? turmas.findByMotoristaId(usuario.id())
                : vinculos.findTurmasAtivasDoAluno(usuario.id());
    }
}
