package br.com.boragora.api.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** O gateway real contra um servidor HTTP local que responde como o Expo. */
class ExpoPushGatewayTest {

    private HttpServer servidor;
    private volatile String corpoRecebido;
    private volatile int status = 200;
    private volatile String resposta;
    private ExpoPushGateway gateway;

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/--/api/v2/push/send", troca -> {
            corpoRecebido = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();
        URI url = URI.create("http://127.0.0.1:%d/--/api/v2/push/send".formatted(servidor.getAddress().getPort()));
        gateway = new ExpoPushGateway(new ExpoPropriedades(url, Duration.ofSeconds(5)));
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    @Test
    void enviaTokenTituloCorpoEDataEDevolveEnviado() {
        resposta = "{\"data\": {\"status\": \"ok\", \"id\": \"abc\"}}";

        ResultadoDoEnvio resultado = gateway.enviar(mensagem());

        assertThat(resultado).isEqualTo(ResultadoDoEnvio.ENVIADO);
        assertThat(corpoRecebido)
                .contains("\"to\":\"ExponentPushToken[xyz]\"")
                .contains("\"title\":\"Vai de van hoje?\"")
                .contains("\"body\":\"Marque ida e volta\"")
                .contains("\"tipo\":\"LEMBRETE\"")
                .contains("\"turmaId\":7");
    }

    @Test
    void deviceNotRegisteredDevolveTokenInvalido() {
        resposta = """
                {"data": {"status": "error", "message": "not registered",
                          "details": {"error": "DeviceNotRegistered"}}}
                """;
        assertThat(gateway.enviar(mensagem())).isEqualTo(ResultadoDoEnvio.TOKEN_INVALIDO);
    }

    @Test
    void outrosErrosDoExpoViramExcecao() {
        resposta = "{\"data\": {\"status\": \"error\", \"message\": \"MessageRateExceeded\"}}";
        assertThatThrownBy(() -> gateway.enviar(mensagem())).hasMessageContaining("MessageRateExceeded");

        status = 500;
        resposta = "{}";
        assertThatThrownBy(() -> gateway.enviar(mensagem())).isInstanceOf(RuntimeException.class);
    }

    private static MensagemPush mensagem() {
        return new MensagemPush("ExponentPushToken[xyz]", "Vai de van hoje?", "Marque ida e volta",
                Map.of("tipo", "LEMBRETE", "turmaId", 7L));
    }
}
