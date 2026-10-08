package br.com.boragora.api.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.compartilhado.RegraDeNegocioException;
import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.grupo.Periodo;
import br.com.boragora.api.turma.Turma;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.Usuario;

/** Regras RN10, RN11 e RN12. Cada teste desfaz o que gravou. */
@TesteDeIntegracao
@Transactional
class ControleDeAcessoTest {

    @Autowired
    private ControleDeAcesso controle;
    @Autowired
    private DadosDeTeste dados;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;

    private Grupo noite;
    private Grupo manha;
    private Turma vanDoCarlos;
    private Turma vanDoJoao;
    private Turma vanDaManha;
    private Usuario carlos;
    private Usuario ana;
    private Usuario alunoRemovido;

    @BeforeEach
    void montarCenario() {
        noite = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));
        manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 30), LocalTime.of(12, 0)));
        carlos = dados.motorista();
        vanDoCarlos = turmas.save(new Turma(noite, carlos, "Van do Carlos", 1, 15));
        vanDoJoao = turmas.save(new Turma(noite, dados.motorista(), "Van do João", 2, 15));
        vanDaManha = turmas.save(new Turma(manha, dados.motorista(), "Van da manhã", 3, 15));
        ana = dados.aluno();
        vinculos.save(new TurmaAluno(vanDoCarlos, ana));
        alunoRemovido = dados.aluno();
        TurmaAluno removido = new TurmaAluno(vanDoCarlos, alunoRemovido);
        removido.setAtivo(false);
        vinculos.save(removido);
    }

    @Test
    void alunoVeOsGruposComVinculoAtivoETodasAsTurmasDeles() {
        UsuarioAutenticado aluno = UsuarioAutenticado.de(ana);

        assertThat(controle.podeVerGrupo(aluno, noite.getId())).isTrue();
        assertThat(controle.podeVerTurma(aluno, vanDoCarlos.getId())).isTrue();
        assertThat(controle.podeVerTurma(aluno, vanDoJoao.getId())).isTrue();
        assertThat(controle.podeVerGrupo(aluno, manha.getId())).isFalse();
        assertThat(controle.podeVerTurma(aluno, vanDaManha.getId())).isFalse();
    }

    @Test
    void alunoComVinculoInativoNaoVeOGrupo() {
        UsuarioAutenticado aluno = UsuarioAutenticado.de(alunoRemovido);

        assertThat(controle.podeVerGrupo(aluno, noite.getId())).isFalse();
        assertThat(controle.podeVerTurma(aluno, vanDoCarlos.getId())).isFalse();
    }

    @Test
    void alunoSemVinculoNaoVeNada() {
        UsuarioAutenticado aluno = UsuarioAutenticado.de(dados.aluno());

        assertThat(controle.podeVerGrupo(aluno, noite.getId())).isFalse();
        assertThat(controle.podeVerGrupo(aluno, manha.getId())).isFalse();
    }

    @Test
    void motoristaVeOsGruposEmQueConduzETodasAsTurmasDeles() {
        UsuarioAutenticado motorista = UsuarioAutenticado.de(carlos);

        assertThat(controle.podeVerGrupo(motorista, noite.getId())).isTrue();
        assertThat(controle.podeVerTurma(motorista, vanDoCarlos.getId())).isTrue();
        assertThat(controle.podeVerTurma(motorista, vanDoJoao.getId())).isTrue();
        assertThat(controle.podeVerGrupo(motorista, manha.getId())).isFalse();
        assertThat(controle.podeVerTurma(motorista, vanDaManha.getId())).isFalse();
    }

    @Test
    void motoristaChefeVeTudo() {
        UsuarioAutenticado chefe = UsuarioAutenticado.de(dados.admin());

        assertThat(controle.podeVerGrupo(chefe, noite.getId())).isTrue();
        assertThat(controle.podeVerGrupo(chefe, manha.getId())).isTrue();
        assertThat(controle.podeVerTurma(chefe, vanDaManha.getId())).isTrue();
    }

    @Test
    void turmaInexistenteNaoEVisivelParaQuemNaoEAdmin() {
        assertThat(controle.podeVerTurma(UsuarioAutenticado.de(ana), Long.MAX_VALUE)).isFalse();
    }

    @Test
    void exigirAcessoSemPermissaoLanca403() {
        UsuarioAutenticado aluno = UsuarioAutenticado.de(ana);

        assertThatThrownBy(() -> controle.exigirAcessoAoGrupo(aluno, manha.getId()))
                .isInstanceOfSatisfying(RegraDeNegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("ACESSO_NEGADO");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        assertThatThrownBy(() -> controle.exigirAcessoATurma(aluno, vanDaManha.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
