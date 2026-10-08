package br.com.boragora.api.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Papel;

/**
 * Único lugar com as regras de visibilidade por recurso (plano, seção 2.4):
 * <ul>
 * <li>aluno vê os grupos em que tem vínculo ativo e todas as turmas desses grupos (RN10);</li>
 * <li>motorista vê os grupos em que conduz alguma turma e todas as turmas desses grupos (RN11);</li>
 * <li>motorista-chefe vê tudo (RN12).</li>
 * </ul>
 * Quem chama confere antes se o recurso existe (404); aqui só se decide o 403.
 */
@Component
@Transactional(readOnly = true)
public class ControleDeAcesso {

    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;

    public ControleDeAcesso(TurmaRepository turmas, TurmaAlunoRepository vinculos) {
        this.turmas = turmas;
        this.vinculos = vinculos;
    }

    public boolean podeVerGrupo(UsuarioAutenticado usuario, Long grupoId) {
        if (usuario.ehAdmin()) {
            return true;
        }
        if (usuario.papel() == Papel.MOTORISTA) {
            return turmas.existsByMotoristaIdAndGrupoId(usuario.id(), grupoId);
        }
        return vinculos.existsByAlunoIdAndAtivoTrueAndTurmaGrupoId(usuario.id(), grupoId);
    }

    public boolean podeVerTurma(UsuarioAutenticado usuario, Long turmaId) {
        if (usuario.ehAdmin()) {
            return true;
        }
        return turmas.findGrupoIdById(turmaId)
                .map(grupoId -> podeVerGrupo(usuario, grupoId))
                .orElse(false);
    }

    public void exigirAcessoAoGrupo(UsuarioAutenticado usuario, Long grupoId) {
        if (!podeVerGrupo(usuario, grupoId)) {
            throw acessoNegado();
        }
    }

    public void exigirAcessoATurma(UsuarioAutenticado usuario, Long turmaId) {
        if (!podeVerTurma(usuario, turmaId)) {
            throw acessoNegado();
        }
    }

    private static RegraDeNegocioException acessoNegado() {
        return new RegraDeNegocioException("ACESSO_NEGADO", HttpStatus.FORBIDDEN,
                RespostasDeSeguranca.ACESSO_NEGADO.mensagem());
    }
}
