package br.com.boragora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Papel;
import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.viagem.Confirmacao;
import br.com.boragora.api.viagem.ConfirmacaoRepository;
import br.com.boragora.api.viagem.StatusVolta;
import br.com.boragora.api.viagem.Viagem;
import br.com.boragora.api.viagem.ViagemRepository;
import jakarta.persistence.EntityManager;

/** Confere o esquema criado pela migração V1: mapeamento, padrões, unicidades e cascatas. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EsquemaDoBancoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 17, 0);

    @Autowired
    private EntityManager em;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;
    @Autowired
    private ViagemRepository viagens;
    @Autowired
    private ConfirmacaoRepository confirmacoes;

    @Test
    void gravaELeCadaEntidade() {
        Usuario motorista = usuarios.save(new Usuario("Carlos", "carlos@teste.com", "hash", Papel.MOTORISTA));
        Usuario aluno = usuarios.save(new Usuario("Ana", "ana@teste.com", "hash", Papel.ALUNO));
        Grupo grupo = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
        Turma turma = turmas.save(new Turma(grupo, motorista, "Van da Zona Sul", 2, 15));
        TurmaAluno vinculo = vinculos.save(new TurmaAluno(turma, aluno));
        Viagem viagem = viagens.save(new Viagem(turma, HOJE));
        Confirmacao confirmacao = new Confirmacao(viagem, vinculo, AGORA);
        confirmacao.setIncluiIda(true);
        confirmacao.setStatusVolta(StatusVolta.LIBERADO);
        confirmacoes.save(confirmacao);
        em.flush();
        em.clear();

        Usuario motoristaLido = usuarios.findById(motorista.getId()).orElseThrow();
        assertThat(motoristaLido.getNomePessoa()).isEqualTo("Carlos");
        assertThat(motoristaLido.getEmail()).isEqualTo("carlos@teste.com");
        assertThat(motoristaLido.getPapel()).isEqualTo(Papel.MOTORISTA);
        assertThat(motoristaLido.isAdmin()).isFalse();
        assertThat(motoristaLido.isAtivo()).isTrue();
        assertThat(motoristaLido.getExpoPushToken()).isNull();

        Grupo grupoLido = grupos.findById(grupo.getId()).orElseThrow();
        assertThat(grupoLido.getPeriodo()).isEqualTo(Periodo.NOITE);
        assertThat(grupoLido.getHorarioSaidaIda()).isEqualTo(LocalTime.of(18, 10));
        assertThat(grupoLido.getHorarioSaidaVolta()).isEqualTo(LocalTime.of(22, 30));

        Turma turmaLida = turmas.findById(turma.getId()).orElseThrow();
        assertThat(turmaLida.getNome()).isEqualTo("Van da Zona Sul");
        assertThat(turmaLida.getNumeroVan()).isEqualTo(2);
        assertThat(turmaLida.getCapacidade()).isEqualTo(15);
        assertThat(turmaLida.getGrupo().getId()).isEqualTo(grupo.getId());
        assertThat(turmaLida.getMotorista().getId()).isEqualTo(motorista.getId());

        TurmaAluno vinculoLido = vinculos.findById(vinculo.getId()).orElseThrow();
        assertThat(vinculoLido.getTurma().getId()).isEqualTo(turma.getId());
        assertThat(vinculoLido.getAluno().getId()).isEqualTo(aluno.getId());
        assertThat(vinculoLido.isAtivo()).isTrue();

        Viagem viagemLida = viagens.findById(viagem.getId()).orElseThrow();
        assertThat(viagemLida.getData()).isEqualTo(HOJE);
        assertThat(viagemLida.getTurma().getId()).isEqualTo(turma.getId());
        assertThat(viagemLida.getViagemAbsorvidaPor()).isNull();

        Confirmacao confirmacaoLida = confirmacoes.findById(confirmacao.getId()).orElseThrow();
        assertThat(confirmacaoLida.getViagem().getId()).isEqualTo(viagem.getId());
        assertThat(confirmacaoLida.getTurmaAluno().getId()).isEqualTo(vinculo.getId());
        assertThat(confirmacaoLida.isIncluiIda()).isTrue();
        assertThat(confirmacaoLida.isIncluiVolta()).isFalse();
        assertThat(confirmacaoLida.getStatusVolta()).isEqualTo(StatusVolta.LIBERADO);
        assertThat(confirmacaoLida.isCancelouAposSaida()).isFalse();
        assertThat(confirmacaoLida.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void bancoAplicaOsValoresPadraoDasColunas() {
        em.createNativeQuery("""
                INSERT INTO usuario (nome_pessoa, email, senha_hash, role) VALUES ('Bia', 'bia@teste.com', 'hash', 'ALUNO')
                """).executeUpdate();
        Cenario c = new Cenario();
        em.createNativeQuery("INSERT INTO turma_aluno (id_turma, id_aluno) VALUES (?, (SELECT id FROM usuario WHERE email = 'bia@teste.com'))")
                .setParameter(1, c.turma.getId()).executeUpdate();
        em.createNativeQuery("""
                INSERT INTO confirmacao (id_viagem, id_turma_aluno, atualizado_em)
                VALUES (?, (SELECT MAX(id) FROM turma_aluno), '2026-10-05 17:00:00')
                """).setParameter(1, c.viagem.getId()).executeUpdate();

        Object[] usuario = (Object[]) em.createNativeQuery(
                "SELECT admin, ativo FROM usuario WHERE email = 'bia@teste.com'").getSingleResult();
        assertThat(usuario).containsExactly(false, true);

        Object ativoVinculo = em.createNativeQuery("SELECT ativo FROM turma_aluno WHERE id = (SELECT MAX(id) FROM turma_aluno)")
                .getSingleResult();
        assertThat(ativoVinculo).isEqualTo(true);

        Object[] confirmacao = (Object[]) em.createNativeQuery("""
                SELECT inclui_ida, inclui_volta, status_volta, cancelou_apos_saida FROM confirmacao
                WHERE id_turma_aluno = (SELECT MAX(id) FROM turma_aluno)
                """).getSingleResult();
        assertThat(confirmacao).containsExactly(false, false, "EM_AULA", false);
    }

    @Test
    void numeroDaVanECapacidadeSaoOpcionais() {
        Usuario motorista = usuarios.save(new Usuario("Carlos", "carlos@teste.com", "hash", Papel.MOTORISTA));
        Grupo grupo = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 30), LocalTime.of(12, 0)));
        Turma turma = turmas.saveAndFlush(new Turma(grupo, motorista, "Van sem cadastro", null, null));
        em.clear();

        Turma lida = turmas.findById(turma.getId()).orElseThrow();
        assertThat(lida.getNumeroVan()).isNull();
        assertThat(lida.getCapacidade()).isNull();
    }

    @Test
    void emailDeUsuarioEhUnico() {
        usuarios.saveAndFlush(new Usuario("Ana", "ana@teste.com", "hash", Papel.ALUNO));

        assertThatThrownBy(() -> usuarios.saveAndFlush(new Usuario("Outra Ana", "ana@teste.com", "hash", Papel.ALUNO)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_usuario_email");
    }

    @Test
    void motoristaNaoConduzDuasTurmasDoMesmoGrupo() {
        Cenario c = new Cenario();

        assertThatThrownBy(() -> turmas.saveAndFlush(new Turma(c.grupo, c.motorista, "Outra van", 3, 15)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_turma_grupo_motorista");
    }

    @Test
    void alunoTemUmVinculoPorTurma() {
        Cenario c = new Cenario();

        assertThatThrownBy(() -> vinculos.saveAndFlush(new TurmaAluno(c.turma, c.aluno)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_turma_aluno_turma_aluno");
    }

    @Test
    void turmaTemUmaViagemPorDia() {
        Cenario c = new Cenario();

        assertThatThrownBy(() -> viagens.saveAndFlush(new Viagem(c.turma, HOJE)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_viagem_turma_data");
    }

    @Test
    void vinculoTemUmaConfirmacaoPorViagem() {
        Cenario c = new Cenario();

        assertThatThrownBy(() -> confirmacoes.saveAndFlush(new Confirmacao(c.viagem, c.vinculo, AGORA)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_confirmacao_viagem_turma_aluno");
    }

    @Test
    void apagarTurmaApagaVinculosViagensEConfirmacoes() {
        Cenario c = new Cenario();
        Cenario outra = new Cenario("outra");
        em.clear();

        turmas.deleteById(c.turma.getId());
        em.flush();
        em.clear();

        assertThat(turmas.existsById(c.turma.getId())).isFalse();
        assertThat(vinculos.existsById(c.vinculo.getId())).isFalse();
        assertThat(viagens.existsById(c.viagem.getId())).isFalse();
        assertThat(confirmacoes.existsById(c.confirmacao.getId())).isFalse();
        // Contas, grupo e dados de outras turmas continuam.
        assertThat(usuarios.existsById(c.motorista.getId())).isTrue();
        assertThat(usuarios.existsById(c.aluno.getId())).isTrue();
        assertThat(grupos.existsById(c.grupo.getId())).isTrue();
        assertThat(confirmacoes.existsById(outra.confirmacao.getId())).isTrue();
    }

    @Test
    void bancoRejeitaApagarGrupoComTurmas() {
        Cenario c = new Cenario();
        em.clear();

        assertThatThrownBy(() -> {
            grupos.deleteById(c.grupo.getId());
            grupos.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_turma_grupo");
    }

    @Test
    void apagarViagemQueAbsorveuOutraDeixaAReferenciaNula() {
        Cenario c = new Cenario();
        Cenario outra = new Cenario("outra");
        em.createNativeQuery("UPDATE viagem SET id_viagem_absorvida_por = ? WHERE id = ?")
                .setParameter(1, outra.viagem.getId())
                .setParameter(2, c.viagem.getId())
                .executeUpdate();

        em.createNativeQuery("DELETE FROM viagem WHERE id = ?").setParameter(1, outra.viagem.getId()).executeUpdate();
        em.clear();

        Viagem viagem = viagens.findById(c.viagem.getId()).orElseThrow();
        assertThat(viagem.getViagemAbsorvidaPor()).isNull();
    }

    /** Um grupo com uma turma, um aluno vinculado, a viagem de hoje e a confirmação do aluno. */
    private class Cenario {
        final Usuario motorista;
        final Usuario aluno;
        final Grupo grupo;
        final Turma turma;
        final TurmaAluno vinculo;
        final Viagem viagem;
        final Confirmacao confirmacao;

        Cenario() {
            this("um");
        }

        Cenario(String sufixo) {
            motorista = usuarios.save(new Usuario("Motorista " + sufixo, "motorista-" + sufixo + "@teste.com", "hash", Papel.MOTORISTA));
            aluno = usuarios.save(new Usuario("Aluno " + sufixo, "aluno-" + sufixo + "@teste.com", "hash", Papel.ALUNO));
            grupo = grupos.save(new Grupo("Grupo " + sufixo, Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
            turma = turmas.save(new Turma(grupo, motorista, "Van " + sufixo, 1, 15));
            vinculo = vinculos.save(new TurmaAluno(turma, aluno));
            viagem = viagens.save(new Viagem(turma, HOJE));
            confirmacao = confirmacoes.save(new Confirmacao(viagem, vinculo, AGORA));
            em.flush();
        }
    }
}
