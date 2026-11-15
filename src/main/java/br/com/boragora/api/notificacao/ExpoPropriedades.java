package br.com.boragora.api.notificacao;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Endereço do Expo Push Service e o tempo máximo de espera, para um Expo lento não segurar a requisição do aluno. */
@ConfigurationProperties("app.expo")
public record ExpoPropriedades(
        @DefaultValue("https://exp.host/--/api/v2/push/send") URI url,
        @DefaultValue("5s") Duration timeout) {
}
