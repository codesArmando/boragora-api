package br.com.boragora.api.notificacao;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.viagem.IdaCanceladaAposSaida;

/**
 * Avisa o motorista responsável pela turma (ponto 3 da seção 1) quando um aluno cancela a ida
 * depois da saída (RN05). Roda só depois do commit do cancelamento, em transação própria; nada que
 * aconteça aqui desfaz o cancelamento.
 */
@Component
public class PushDeCancelamentoTardio {

    private static final Logger log = LoggerFactory.getLogger(PushDeCancelamentoTardio.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurmaRepository turmas;
    private final UsuarioRepository usuarios;
    private final EnvioDePush envio;

    public PushDeCancelamentoTardio(TurmaRepository turmas, UsuarioRepository usuarios, EnvioDePush envio) {
        this.turmas = turmas;
        this.usuarios = usuarios;
        this.envio = envio;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoCancelarIdaAposSaida(IdaCanceladaAposSaida evento) {
        try {
            Turma turma = turmas.findComGrupoEMotorista(evento.turmaId()).orElse(null);
            Usuario aluno = usuarios.findById(evento.alunoId()).orElse(null);
            if (turma == null || aluno == null) {
                return;
            }
            Grupo grupo = turma.getGrupo();
            envio.enviar(turma.getMotorista(),
                    "%s cancelou a ida para a faculdade!".formatted(aluno.getNomePessoa()),
                    "%s · %s · depois da saída das %s".formatted(turma.getNome(), grupo.getNome(),
                            grupo.getHorarioSaidaIda().format(HORA)),
                    TipoDePush.CANCELAMENTO_TARDIO, turma.getId());
        } catch (RuntimeException e) {
            log.warn("Falha ao preparar o push de cancelamento tardio da turma {}", evento.turmaId(), e);
        }
    }
}
