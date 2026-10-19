package br.com.boragora.api.viagem;

import java.time.LocalDateTime;

import br.com.boragora.api.turma.TurmaAluno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Plano do dia de um aluno em uma viagem. */
@Entity
@Table(name = "confirmacao")
public class Confirmacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_viagem", nullable = false, updatable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turma_aluno", nullable = false, updatable = false)
    private TurmaAluno turmaAluno;

    @Column(name = "inclui_ida", nullable = false)
    private boolean incluiIda = false;

    @Column(name = "inclui_volta", nullable = false)
    private boolean incluiVolta = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_volta", nullable = false, length = 10)
    private StatusVolta statusVolta = StatusVolta.EM_AULA;

    @Column(name = "cancelou_apos_saida", nullable = false)
    private boolean cancelouAposSaida = false;

    /** Preenchido pelo service com o horário do {@link java.time.Clock} injetado. */
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Confirmacao() {
    }

    public Confirmacao(Viagem viagem, TurmaAluno turmaAluno, LocalDateTime atualizadoEm) {
        this.viagem = viagem;
        this.turmaAluno = turmaAluno;
        this.atualizadoEm = atualizadoEm;
    }

    public Long getId() {
        return id;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public TurmaAluno getTurmaAluno() {
        return turmaAluno;
    }

    public boolean isIncluiIda() {
        return incluiIda;
    }

    public void setIncluiIda(boolean incluiIda) {
        this.incluiIda = incluiIda;
    }

    public boolean isIncluiVolta() {
        return incluiVolta;
    }

    public void setIncluiVolta(boolean incluiVolta) {
        this.incluiVolta = incluiVolta;
    }

    public StatusVolta getStatusVolta() {
        return statusVolta;
    }

    public void setStatusVolta(StatusVolta statusVolta) {
        this.statusVolta = statusVolta;
    }

    public boolean isCancelouAposSaida() {
        return cancelouAposSaida;
    }

    public void setCancelouAposSaida(boolean cancelouAposSaida) {
        this.cancelouAposSaida = cancelouAposSaida;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
