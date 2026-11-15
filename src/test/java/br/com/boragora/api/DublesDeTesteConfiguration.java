package br.com.boragora.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Põe o {@link RelogioDeTeste} no lugar do relógio da aplicação e o {@link PushGatewayFalso} no lugar do Expo. */
@TestConfiguration(proxyBeanMethods = false)
public class DublesDeTesteConfiguration {

    @Bean
    @Primary
    RelogioDeTeste relogioDeTeste() {
        return new RelogioDeTeste();
    }

    @Bean
    @Primary
    PushGatewayFalso pushGatewayFalso() {
        return new PushGatewayFalso();
    }
}
