package br.com.boragora.api.turma;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.usuario.Usuario;

/**
 * Único lugar da RN08: ninguém participa de duas turmas do mesmo grupo. As restrições
 * únicas do banco são a segunda barreira, para gravações simultâneas.
 */
@Component
@Transactional(readOnly = true)
public class ValidadorConflitoDeGrupo {

    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;

    public ValidadorConflitoDeGrupo(TurmaRepository turmas, TurmaAlunoRepository vinculos) {
        this.turmas = turmas;
        this.vinculos = vinculos;
    }

    /** Usado na criação da turma e na troca de motorista. */
    public void validarMotorista(Long motoristaId, Long grupoId) {
        turmas.findByMotoristaIdAndGrupoId(motoristaId, grupoId).ifPresent(turma -> {
            throw conflitoMotorista("Este motorista já conduz a turma \"%s\" neste grupo.".formatted(turma.getNome()));
        });
    }

    /**
     * Usado no vínculo de alunos. O banco não tem restrição única de aluno por grupo; quem
     * evita a gravação simultânea é a trava nas linhas dos alunos, feita por quem vincula.
     */
    public void validarAluno(Usuario aluno, Long grupoId) {
        vinculos.findTurmasAtivasDoAlunoNoGrupo(aluno.getId(), grupoId).stream().findFirst().ifPresent(turma -> {
            throw new RegraDeNegocioException("CONFLITO_GRUPO_ALUNO", HttpStatus.CONFLICT,
                    "%s já está na turma \"%s\" deste grupo.".formatted(aluno.getNomePessoa(), turma.getNome()));
        });
    }

    /** Mensagem genérica para quando quem detecta o conflito é a restrição única do banco. */
    static RegraDeNegocioException conflitoMotorista() {
        return conflitoMotorista("Este motorista já conduz outra turma neste grupo.");
    }

    private static RegraDeNegocioException conflitoMotorista(String mensagem) {
        return new RegraDeNegocioException("CONFLITO_GRUPO_MOTORISTA", HttpStatus.CONFLICT, mensagem);
    }
}
