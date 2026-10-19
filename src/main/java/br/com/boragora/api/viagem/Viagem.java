package br.com.boragora.api.viagem;

import java.time.LocalDate;

import br.com.boragora.api.turma.Turma;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Viagem de uma turma em um dia, criada na primeira confirmação do dia. */
@Entity
@Table(name = "viagem")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turma", nullable = false, updatable = false)
    private Turma turma;

    @Column(nullable = false, updatable = false)
    private LocalDate data;

    /** Reservado para a unificação de turmas (seção 9.1 do escopo). Nenhuma funcionalidade usa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_viagem_absorvida_por")
    private Viagem viagemAbsorvidaPor;

    protected Viagem() {
    }

    public Viagem(Turma turma, LocalDate data) {
        this.turma = turma;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public Turma getTurma() {
        return turma;
    }

    public LocalDate getData() {
        return data;
    }

    public Viagem getViagemAbsorvidaPor() {
        return viagemAbsorvidaPor;
    }
}
