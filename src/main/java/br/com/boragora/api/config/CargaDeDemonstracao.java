package br.com.boragora.api.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Cenário das telas do design, para o app mostrar o mesmo que o protótipo. Liga com
 * {@code app.demonstracao.carga = true} (só no perfil {@code dev}) e roda uma vez: se algum
 * e-mail da carga já existe no banco, não faz nada.
 * <ul>
 * <li>Grupos Manhã (06:20 / 12:10) e Noite (18:10 / 22:30), cada um com Van 1 (Carlos Menezes)
 * e Van 2 (Vanderlei Dias);</li>
 * <li>João Gabriel em Manhã · Van 1 e Noite · Van 2; Noite · Van 2 com os oito alunos da tela C4;</li>
 * <li>Ana Beatriz Moura, Diego Farias e Vitor Sampaio sem vínculo (telas D6 e D10);</li>
 * <li>confirmações do dia da carga na Noite · Van 2, com os contadores da tela C1: 5 vão,
 * 7 voltam, 2 liberados e 1 cancelamento tardio (Camila Rocha).</li>
 * </ul>
 * Todos os usuários da carga usam a senha {@value #SENHA}.
 */
@Component
@ConditionalOnProperty(name = "app.demonstracao.carga", havingValue = "true")
public class CargaDeDemonstracao implements ApplicationRunner {

    public static final String SENHA = "senha123";
    private static final List<String> EMAILS = List.of("carlos", "vanderlei", "joao", "beatriz", "camila",
            "larissa", "lucas", "mariana", "pedro", "rafael", "ana", "diego", "vitor").stream()
            .map(nome -> nome + "@boragora.local")
            .toList();

    private static final Logger log = LoggerFactory.getLogger(CargaDeDemonstracao.class);

    private final UsuarioRepository usuarios;
    private final GrupoRepository grupos;
    private final TurmaRepository turmas;
    private final TurmaAlunoRepository vinculos;
    private final ViagemRepository viagens;
    private final ConfirmacaoRepository confirmacoes;
    private final PasswordEncoder senhas;
    private final Clock relogio;

    public CargaDeDemonstracao(UsuarioRepository usuarios, GrupoRepository grupos, TurmaRepository turmas,
            TurmaAlunoRepository vinculos, ViagemRepository viagens, ConfirmacaoRepository confirmacoes,
            PasswordEncoder senhas, Clock relogio) {
        this.usuarios = usuarios;
        this.grupos = grupos;
        this.turmas = turmas;
        this.vinculos = vinculos;
        this.viagens = viagens;
        this.confirmacoes = confirmacoes;
        this.senhas = senhas;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (EMAILS.stream().anyMatch(usuarios::existsByEmail)) {
            log.info("Carga de demonstração ignorada: os usuários da carga já existem no banco.");
            return;
        }
        Grupo manha = grupos.save(new Grupo("Manhã", Periodo.MANHA, LocalTime.of(6, 20), LocalTime.of(12, 10)));
        Grupo noite = grupos.save(new Grupo("Noite", Periodo.NOITE, LocalTime.of(18, 10), LocalTime.of(22, 30)));

        Usuario carlos = usuario("Carlos Menezes", "carlos", Papel.MOTORISTA);
        Usuario vanderlei = usuario("Vanderlei Dias", "vanderlei", Papel.MOTORISTA);
        Turma manhaVan1 = turmas.save(new Turma(manha, carlos, "Van 1", 1, 15));
        turmas.save(new Turma(manha, vanderlei, "Van 2", 2, 15));
        turmas.save(new Turma(noite, carlos, "Van 1", 1, 15));
        Turma noiteVan2 = turmas.save(new Turma(noite, vanderlei, "Van 2", 2, 15));

        Usuario joao = usuario("João Gabriel", "joao", Papel.ALUNO);
        vinculos.save(new TurmaAluno(manhaVan1, joao));

        LocalDateTime agora = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
        Viagem viagem = viagens.save(new Viagem(noiteVan2, agora.toLocalDate()));
        // Plano de hoje de cada aluno da Noite · Van 2. Rafael Lima não declarou nada.
        List<AlunoDaCarga> alunosDaNoite = List.of(
                new AlunoDaCarga("Beatriz Souza", new Plano(true, true, StatusVolta.LIBERADO, false)),
                new AlunoDaCarga("Camila Rocha", new Plano(false, true, StatusVolta.EM_AULA, true)),
                new AlunoDaCarga("João Gabriel", new Plano(true, true, StatusVolta.EM_AULA, false)),
                new AlunoDaCarga("Larissa Costa", new Plano(true, true, StatusVolta.LIBERADO, false)),
                new AlunoDaCarga("Lucas Pereira", new Plano(true, true, StatusVolta.EM_AULA, false)),
                new AlunoDaCarga("Mariana Alves", new Plano(true, true, StatusVolta.EM_AULA, false)),
                new AlunoDaCarga("Pedro Henrique Dias", new Plano(false, true, StatusVolta.EM_AULA, false)),
                new AlunoDaCarga("Rafael Lima", null));
        for (AlunoDaCarga item : alunosDaNoite) {
            Usuario aluno = item.nome().equals(joao.getNomePessoa()) ? joao
                    : usuario(item.nome(), primeiroNome(item.nome()), Papel.ALUNO);
            TurmaAluno vinculo = vinculos.save(new TurmaAluno(noiteVan2, aluno));
            if (item.plano() != null) {
                confirmacoes.save(item.plano().confirmacao(viagem, vinculo, agora));
            }
        }

        usuario("Ana Beatriz Moura", "ana", Papel.ALUNO);
        usuario("Diego Farias", "diego", Papel.ALUNO);
        usuario("Vitor Sampaio", "vitor", Papel.ALUNO);
        log.info("Carga de demonstração criada, com confirmações de {}. Senha dos usuários: {}",
                agora.toLocalDate(), SENHA);
    }

    private Usuario usuario(String nome, String email, Papel papel) {
        return usuarios.save(new Usuario(nome, email + "@boragora.local", senhas.encode(SENHA), papel));
    }

    /** "Pedro Henrique Dias" vira "pedro"; sem acentos para o e-mail. */
    private static String primeiroNome(String nome) {
        return nome.split(" ")[0].toLowerCase(Locale.ROOT);
    }

    private record AlunoDaCarga(String nome, Plano plano) {
    }

    private record Plano(boolean ida, boolean volta, StatusVolta status, boolean cancelouAposSaida) {

        Confirmacao confirmacao(Viagem viagem, TurmaAluno vinculo, LocalDateTime agora) {
            Confirmacao confirmacao = new Confirmacao(viagem, vinculo, agora);
            confirmacao.setIncluiIda(ida);
            confirmacao.setIncluiVolta(volta);
            confirmacao.setStatusVolta(status);
            confirmacao.setCancelouAposSaida(cancelouAposSaida);
            return confirmacao;
        }
    }
}
