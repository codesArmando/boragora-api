package br.com.boragora.api.notificacao;

import java.time.DayOfWeek;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Lembrete do plano do dia (RF13). {@code agendado = false} desliga a tarefa a cada minuto (usado
 * nos testes, que chamam o lembrete diretamente com o relógio fixo).
 */
@ConfigurationProperties("app.lembrete")
public record LembretePropriedades(
        @DefaultValue("true") boolean agendado,
        @DefaultValue("120") int antecedenciaMinutos,
        @DefaultValue({ "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" }) Set<DayOfWeek> dias) {

    public LembretePropriedades {
        if (antecedenciaMinutos <= 0) {
            throw new IllegalStateException("app.lembrete.antecedencia-minutos precisa ser maior que zero.");
        }
        dias = Set.copyOf(dias);
    }
}
