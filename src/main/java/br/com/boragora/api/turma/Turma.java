package br.com.boragora.api.turma;

import br.com.boragora.api.grupo.Grupo;
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

@Entity
@Table(name = "turma")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** O grupo não muda depois que a turma é criada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_grupo", nullable = false, updatable = false)
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_motorista", nullable = false)
    private Usuario motorista;

    @Column(nullable = false, length = 60)
    private String nome;

    /** Dado cadastral opcional, sem uso em regras. */
    @Column(name = "numero_van")
    private Integer numeroVan;

    /** Dado cadastral opcional, sem uso em regras. */
    private Integer capacidade;

    protected Turma() {
    }

    public Turma(Grupo grupo, Usuario motorista, String nome, Integer numeroVan, Integer capacidade) {
        this.grupo = grupo;
        this.motorista = motorista;
        this.nome = nome;
        this.numeroVan = numeroVan;
        this.capacidade = capacidade;
    }

    public Long getId() {
        return id;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public Usuario getMotorista() {
        return motorista;
    }

    public void setMotorista(Usuario motorista) {
        this.motorista = motorista;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getNumeroVan() {
        return numeroVan;
    }

    public void setNumeroVan(Integer numeroVan) {
        this.numeroVan = numeroVan;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }
}
