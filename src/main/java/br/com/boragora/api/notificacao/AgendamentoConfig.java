package br.com.boragora.api.notificacao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga a tarefa do lembrete, a menos que {@code app.lembrete.agendado = false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.lembrete.agendado", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {
}
