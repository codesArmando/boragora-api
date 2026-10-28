package br.com.boragora.api.turma;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.compartilhado.RegraDeNegocioException;

/**
 * Único lugar da RN08: ninguém participa de duas turmas do mesmo grupo. As restrições
 * únicas do banco são a segunda barreira, para gravações simultâneas.
 */
@Component
@Transactional(readOnly = true)
public class ValidadorConflitoDeGrupo {

    private final TurmaRepository turmas;

    public ValidadorConflitoDeGrupo(TurmaRepository turmas) {
        this.turmas = turmas;
    }

    /** Usado na criação da turma e na troca de motorista. */
    public void validarMotorista(Long motoristaId, Long grupoId) {
        turmas.findByMotoristaIdAndGrupoId(motoristaId, grupoId).ifPresent(turma -> {
            throw conflitoMotorista("Este motorista já conduz a turma \"%s\" neste grupo.".formatted(turma.getNome()));
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
