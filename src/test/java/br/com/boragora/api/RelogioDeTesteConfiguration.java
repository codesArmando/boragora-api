package br.com.boragora.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Põe o {@link RelogioDeTeste} no lugar do relógio da aplicação. */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioDeTesteConfiguration {

    @Bean
    @Primary
    RelogioDeTeste relogioDeTeste() {
        return new RelogioDeTeste();
    }
}
