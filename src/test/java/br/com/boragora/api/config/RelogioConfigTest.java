package br.com.boragora.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RelogioConfigTest {

    @Test
    void relogioFixoCongelaNoHorarioInformadoNoFusoDeSaoPaulo() {
        Clock relogio = RelogioConfig.criarRelogio("2026-10-05T18:30");

        assertThat(relogio.getZone()).isEqualTo(RelogioConfig.FUSO);
        assertThat(LocalDateTime.now(relogio)).isEqualTo(LocalDateTime.of(2026, 10, 5, 18, 30));
        assertThat(relogio.instant()).isEqualTo(relogio.instant());
    }

    @Test
    void semRelogioFixoUsaOHorarioRealNoFusoDeSaoPaulo() {
        Clock relogio = RelogioConfig.criarRelogio("");

        assertThat(relogio.getZone()).isEqualTo(RelogioConfig.FUSO);
        assertThat(relogio.instant()).isCloseTo(java.time.Instant.now(), org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
    }
}
