package br.com.boragora.api.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Relógio único da aplicação. Todo service que depende de hora recebe este {@link Clock}
 * em vez de chamar {@code now()} diretamente.
 */
@Configuration
public class RelogioConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    @Profile("dev")
    public Clock relogioDev(@Value("${app.relogio.fixo:}") String fixo) {
        return criarRelogio(fixo);
    }

    @Bean
    @Profile("!dev")
    public Clock relogio() {
        return Clock.system(FUSO);
    }

    /** Relógio congelado em {@code fixo} (formato {@code yyyy-MM-ddTHH:mm}) ou o relógio real, se vazio. */
    static Clock criarRelogio(String fixo) {
        if (fixo == null || fixo.isBlank()) {
            return Clock.system(FUSO);
        }
        LocalDateTime instante = LocalDateTime.parse(fixo.trim());
        return Clock.fixed(instante.atZone(FUSO).toInstant(), FUSO);
    }
}
