package br.com.boragora.api.grupo;

import java.time.LocalTime;
import java.util.Comparator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grupo")
public class Grupo {

    /** Ordem das telas: período (manhã, tarde, noite), horário de ida e nome. */
    public static final Comparator<Grupo> ORDEM_DE_EXIBICAO = Comparator
            .comparing(Grupo::getPeriodo)
            .thenComparing(Grupo::getHorarioSaidaIda)
            .thenComparing(Grupo::getNome, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Grupo::getId);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Periodo periodo;

    @Column(name = "horario_saida_ida", nullable = false)
    private LocalTime horarioSaidaIda;

    @Column(name = "horario_saida_volta", nullable = false)
    private LocalTime horarioSaidaVolta;

    protected Grupo() {
    }

    public Grupo(String nome, Periodo periodo, LocalTime horarioSaidaIda, LocalTime horarioSaidaVolta) {
        this.nome = nome;
        this.periodo = periodo;
        this.horarioSaidaIda = horarioSaidaIda;
        this.horarioSaidaVolta = horarioSaidaVolta;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public LocalTime getHorarioSaidaIda() {
        return horarioSaidaIda;
    }

    public void setHorarioSaidaIda(LocalTime horarioSaidaIda) {
        this.horarioSaidaIda = horarioSaidaIda;
    }

    public LocalTime getHorarioSaidaVolta() {
        return horarioSaidaVolta;
    }

    public void setHorarioSaidaVolta(LocalTime horarioSaidaVolta) {
        this.horarioSaidaVolta = horarioSaidaVolta;
    }
}
