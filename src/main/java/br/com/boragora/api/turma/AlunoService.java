package br.com.boragora.api.turma;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

@Service
public class AlunoService {

    private final UsuarioRepository usuarios;
    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;

    public AlunoService(UsuarioRepository usuarios, TurmaRepository turmas, TurmaAlunoRepository vinculos) {
        this.usuarios = usuarios;
        this.turmas = turmas;
        this.vinculos = vinculos;
    }

    /**
     * Alunos ativos em ordem alfabética, filtrados por nome ou e-mail. Com {@code turmaId} (tela D6),
     * traz a situação de cada um e omite quem já está na turma; sem ele (tela D10), é a listagem geral.
     * Duas consultas no total: alunos e vínculos ativos deles.
     */
    @Transactional(readOnly = true)
    public List<AlunoAdminResposta> listar(String busca, Long turmaId) {
        Turma turma = turmaId == null ? null : turmas.findById(turmaId)
                .orElseThrow(() -> RegraDeNegocioException.validacao("turmaId", "Turma não encontrada."));
        String filtro = busca == null || busca.isBlank() ? null : busca.strip();
        List<Usuario> alunos = usuarios.buscarAtivos(Papel.ALUNO, filtro);
        Map<Long, List<Turma>> turmasPorAluno = alunos.isEmpty() ? Map.of()
                : vinculos.findAtivosDosAlunos(alunos.stream().map(Usuario::getId).toList()).stream()
                        .collect(Collectors.groupingBy(vinculo -> vinculo.getAluno().getId(),
                                Collectors.mapping(TurmaAluno::getTurma, Collectors.toList())));

        return alunos.stream()
                .map(aluno -> montar(aluno, turmasPorAluno.getOrDefault(aluno.getId(), List.of()), turma))
                .flatMap(Optional::stream)
                .toList();
    }

    /** Vazio quando o aluno já está na própria turma, que não é listado. */
    private static Optional<AlunoAdminResposta> montar(Usuario aluno, List<Turma> atuais, Turma turma) {
        List<Turma> ordenadas = atuais.stream().sorted(TurmaService.ORDEM_CADASTRAL).toList();
        List<TurmaDoAluno> turmasDoAluno = ordenadas.stream().map(TurmaDoAluno::de).toList();
        if (turma == null) {
            return Optional.of(resposta(aluno, turmasDoAluno, null, null));
        }
        if (ordenadas.stream().anyMatch(atual -> atual.getId().equals(turma.getId()))) {
            return Optional.empty();
        }
        Optional<Turma> conflito = ordenadas.stream()
                .filter(atual -> atual.getGrupo().getId().equals(turma.getGrupo().getId()))
                .findFirst();
        return Optional.of(conflito
                .map(emConflito -> resposta(aluno, turmasDoAluno, SituacaoAluno.CONFLITO_NO_GRUPO,
                        TurmaDoAluno.de(emConflito)))
                .orElseGet(() -> resposta(aluno, turmasDoAluno, SituacaoAluno.DISPONIVEL, null)));
    }

    private static AlunoAdminResposta resposta(Usuario aluno, List<TurmaDoAluno> turmas, SituacaoAluno situacao,
            TurmaDoAluno turmaEmConflito) {
        return new AlunoAdminResposta(aluno.getId(), aluno.getNomePessoa(), aluno.getEmail(), turmas, situacao,
                turmaEmConflito);
    }
}
