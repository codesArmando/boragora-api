package br.com.boragora.api.turma;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.seguranca.ControleDeAcesso;
import br.com.boragora.api.seguranca.UsuarioAutenticado;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

@Service
public class TurmaService {

    /** Lista cadastral: grupos na ordem de {@code GET /grupos}, depois o nome da turma. */
    public static final Comparator<Turma> ORDEM_CADASTRAL = Comparator
            .comparing(Turma::getGrupo, Grupo.ORDEM_DE_EXIBICAO)
            .thenComparing(Turma::getNome, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Turma::getId);

    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;
    private final GrupoRepository grupos;
    private final UsuarioRepository usuarios;
    private final ValidadorConflitoDeGrupo validadorConflito;
    private final ControleDeAcesso controleDeAcesso;

    public TurmaService(TurmaRepository turmas, TurmaAlunoRepository vinculos, GrupoRepository grupos,
            UsuarioRepository usuarios, ValidadorConflitoDeGrupo validadorConflito, ControleDeAcesso controleDeAcesso) {
        this.turmas = turmas;
        this.vinculos = vinculos;
        this.grupos = grupos;
        this.usuarios = usuarios;
        this.validadorConflito = validadorConflito;
        this.controleDeAcesso = controleDeAcesso;
    }

    @Transactional
    public TurmaResposta criar(TurmaCriacaoRequisicao requisicao) {
        Grupo grupo = grupos.findById(requisicao.grupoId())
                .orElseThrow(() -> RegraDeNegocioException.validacao("grupoId", "Grupo não encontrado."));
        Usuario motorista = buscarMotorista(requisicao.motoristaId());
        validadorConflito.validarMotorista(motorista.getId(), grupo.getId());
        Turma turma = gravar(new Turma(grupo, motorista, requisicao.nome(),
                requisicao.numeroVan(), requisicao.capacidade()));
        return montarResposta(turma);
    }

    /** Edita os dados cadastrais e troca o motorista. O grupo da turma não muda. */
    @Transactional
    public TurmaResposta atualizar(Long id, TurmaEdicaoRequisicao requisicao) {
        Turma turma = buscar(id);
        Usuario motorista = buscarMotorista(requisicao.motoristaId());
        if (!motorista.getId().equals(turma.getMotorista().getId())) {
            validadorConflito.validarMotorista(motorista.getId(), turma.getGrupo().getId());
            turma.setMotorista(motorista);
        }
        turma.setNome(requisicao.nome());
        turma.setNumeroVan(requisicao.numeroVan());
        turma.setCapacidade(requisicao.capacidade());
        return montarResposta(gravar(turma));
    }

    /** Vínculos, viagens e confirmações da turma são apagados pela cascata do banco (RN17). */
    @Transactional
    public void excluir(Long id) {
        turmas.delete(buscar(id));
        turmas.flush();
    }

    /**
     * Vincula o lote inteiro ou ninguém (RF03). Quem já está ativo na turma é ignorado; vínculo
     * inativo é reativado. As linhas dos alunos ficam travadas até o fim da transação, para que
     * dois vínculos simultâneos do mesmo aluno não furem a RN08.
     */
    @Transactional
    public TurmaResposta vincularAlunos(Long id, VinculoRequisicao requisicao) {
        Turma turma = buscar(id);
        List<Long> ids = requisicao.alunoIds().stream().distinct().toList();
        Map<Long, Usuario> travados = usuarios.travarPorIds(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        List<Usuario> novos = new ArrayList<>();
        List<TurmaAluno> aReativar = new ArrayList<>();
        for (Long alunoId : ids) {
            Usuario aluno = exigirAluno(travados.get(alunoId), alunoId);
            Optional<TurmaAluno> existente = vinculos.findByTurmaIdAndAlunoId(turma.getId(), aluno.getId());
            if (existente.filter(TurmaAluno::isAtivo).isPresent()) {
                continue;
            }
            validadorConflito.validarAluno(aluno, turma.getGrupo().getId());
            existente.ifPresentOrElse(aReativar::add, () -> novos.add(aluno));
        }
        aReativar.forEach(vinculo -> vinculo.setAtivo(true));
        novos.forEach(aluno -> vinculos.save(new TurmaAluno(turma, aluno)));
        vinculos.flush();
        return montarResposta(turma);
    }

    /** Desativa o vínculo; não há exclusão física (RN18). */
    @Transactional
    public void removerAluno(Long id, Long alunoId) {
        buscar(id);
        vinculos.findByTurmaIdAndAlunoId(id, alunoId)
                .filter(TurmaAluno::isAtivo)
                .orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Este aluno não está vinculado a esta turma."))
                .setAtivo(false);
    }

    @Transactional(readOnly = true)
    public TurmaResposta detalhar(Long id, UsuarioAutenticado usuario) {
        Turma turma = buscar(id);
        controleDeAcesso.exigirAcessoATurma(usuario, id);
        return montarResposta(turma);
    }

    /** Lista cadastral da tela D3, com duas consultas no total: turmas e contagem de alunos. */
    @Transactional(readOnly = true)
    public List<TurmaCadastroResposta> listarCadastro() {
        Map<Long, Long> totais = vinculos.contarAtivosPorTurma().stream()
                .collect(Collectors.toMap(TotalDeAlunos::turmaId, TotalDeAlunos::total));
        return turmas.findAllComGrupoEMotorista().stream()
                .sorted(ORDEM_CADASTRAL)
                .map(turma -> new TurmaCadastroResposta(turma.getId(), turma.getNome(), turma.getNumeroVan(),
                        turma.getCapacidade(), GrupoDaTurma.de(turma.getGrupo()),
                        PessoaDaTurma.de(turma.getMotorista()), totais.getOrDefault(turma.getId(), 0L)))
                .toList();
    }

    private Turma buscar(Long id) {
        return turmas.findById(id).orElseThrow(() -> RegraDeNegocioException.naoEncontrado("Turma não encontrada."));
    }

    private Usuario buscarMotorista(Long motoristaId) {
        return usuarios.findById(motoristaId)
                .filter(usuario -> usuario.isAtivo() && usuario.getPapel() == Papel.MOTORISTA)
                .orElseThrow(() -> RegraDeNegocioException.validacao("motoristaId",
                        "O motorista informado não existe ou não está ativo."));
    }

    private static Usuario exigirAluno(Usuario usuario, Long alunoId) {
        if (usuario == null || !usuario.isAtivo() || usuario.getPapel() != Papel.ALUNO) {
            throw RegraDeNegocioException.validacao("alunoIds",
                    "O usuário %d não existe ou não é um aluno ativo.".formatted(alunoId));
        }
        return usuario;
    }

    /** A restrição única (id_grupo, id_motorista) é a segunda barreira da RN08. */
    private Turma gravar(Turma turma) {
        try {
            return turmas.saveAndFlush(turma);
        } catch (DataIntegrityViolationException e) {
            throw ValidadorConflitoDeGrupo.conflitoMotorista();
        }
    }

    private TurmaResposta montarResposta(Turma turma) {
        List<PessoaDaTurma> alunos = vinculos.findAlunosAtivosDaTurma(turma.getId()).stream()
                .map(PessoaDaTurma::de)
                .toList();
        return new TurmaResposta(turma.getId(), turma.getNome(), turma.getNumeroVan(), turma.getCapacidade(),
                GrupoDaTurma.de(turma.getGrupo()), PessoaDaTurma.de(turma.getMotorista()), alunos);
    }
}
