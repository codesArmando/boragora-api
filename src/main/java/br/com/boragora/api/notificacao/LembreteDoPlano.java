package br.com.boragora.api.notificacao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.boragora.api.grupo.Grupo;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.turma.TurmaAluno;
import br.com.boragora.api.turma.TurmaAlunoRepository;

/**
 * Lembra o aluno de declarar o plano (RF13), na antecedência configurada em relação à saída da ida,
 * nos dias configurados (o dia que vale é o da ida). Vai só para quem não marcou ida nem volta.
 * Se o servidor estiver fora do ar no minuto do disparo, o lembrete daquele dia não é enviado.
 */
@Component
public class LembreteDoPlano {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final GrupoRepository grupos;
    private final TurmaAlunoRepository vinculos;
    private final EnvioDePush envio;
    private final LembretePropriedades propriedades;
    private final Clock relogio;

    public LembreteDoPlano(GrupoRepository grupos, TurmaAlunoRepository vinculos, EnvioDePush envio,
            LembretePropriedades propriedades, Clock relogio) {
        this.grupos = grupos;
        this.vinculos = vinculos;
        this.envio = envio;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Scheduled(cron = "0 * * * * *", zone = "America/Sao_Paulo")
    public void enviar() {
        LocalDateTime saida = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(propriedades.antecedenciaMinutos());
        if (!propriedades.dias().contains(saida.getDayOfWeek())) {
            return;
        }
        List<Long> grupoIds = grupos.findByHorarioSaidaIda(saida.toLocalTime()).stream().map(Grupo::getId).toList();
        if (grupoIds.isEmpty()) {
            return;
        }
        String corpo = "Marque ida e volta até %s para entrar na lista do motorista."
                .formatted(saida.toLocalTime().format(HORA));
        for (TurmaAluno vinculo : vinculos.findSemPlanoNoDiaComToken(grupoIds, saida.toLocalDate())) {
            envio.enviar(vinculo.getAluno(), "Vai de van hoje?", corpo, TipoDePush.LEMBRETE,
                    vinculo.getTurma().getId());
        }
    }
}
