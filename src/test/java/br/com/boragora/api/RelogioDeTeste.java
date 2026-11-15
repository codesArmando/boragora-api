package br.com.boragora.api;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import br.com.boragora.api.config.RelogioConfig;

/**
 * Relógio dos testes de integração: segue o relógio real até um teste fixar um horário.
 * Quem fixa volta ao real no fim do teste ({@link #voltarAoReal()}).
 */
public class RelogioDeTeste extends Clock {

    private volatile Clock atual = Clock.system(RelogioConfig.FUSO);

    public void fixar(LocalDateTime instante) {
        atual = Clock.fixed(instante.atZone(RelogioConfig.FUSO).toInstant(), RelogioConfig.FUSO);
    }

    public void voltarAoReal() {
        atual = Clock.system(RelogioConfig.FUSO);
    }

    @Override
    public ZoneId getZone() {
        return atual.getZone();
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return atual.withZone(zona);
    }

    @Override
    public Instant instant() {
        return atual.instant();
    }
}
