package br.com.boragora.api.notificacao;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envio pelo Expo Push Service ({@code POST /--/api/v2/push/send}), uma mensagem por chamada.
 * Só o recibo imediato (ticket) é conferido; os recibos posteriores do Expo não são consultados.
 */
@Component
public class ExpoPushGateway implements PushGateway {

    private final RestClient cliente;

    public ExpoPushGateway(ExpoPropriedades propriedades) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeout()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeout());
        this.cliente = RestClient.builder()
                .baseUrl(propriedades.url().toString())
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public ResultadoDoEnvio enviar(MensagemPush mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("to", mensagem.token());
        corpo.put("title", mensagem.titulo());
        corpo.put("body", mensagem.corpo());
        corpo.put("data", mensagem.data());
        corpo.put("sound", "default");

        RespostaDoExpo resposta = cliente.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(RespostaDoExpo.class);
        Ticket ticket = resposta == null ? null : resposta.data();
        if (ticket == null) {
            throw new IllegalStateException("Resposta do Expo sem ticket.");
        }
        if ("ok".equals(ticket.status())) {
            return ResultadoDoEnvio.ENVIADO;
        }
        if (ticket.details() != null && "DeviceNotRegistered".equals(ticket.details().get("error"))) {
            return ResultadoDoEnvio.TOKEN_INVALIDO;
        }
        throw new IllegalStateException("Expo recusou o push: " + ticket.message());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaDoExpo(Ticket data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ticket(String status, String message, Map<String, Object> details) {
    }
}
