package br.com.boragora.api.turma;

import br.com.boragora.api.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Vínculo do aluno com a turma. Remover o aluno desativa o vínculo; não há exclusão física. */
@Entity
@Table(name = "turma_aluno")
public class TurmaAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turma", nullable = false, updatable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aluno", nullable = false, updatable = false)
    private Usuario aluno;

    @Column(nullable = false)
    private boolean ativo = true;

    protected TurmaAluno() {
    }

    public TurmaAluno(Turma turma, Usuario aluno) {
        this.turma = turma;
        this.aluno = aluno;
    }

    public Long getId() {
        return id;
    }

    public Turma getTurma() {
        return turma;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
