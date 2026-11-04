package br.com.boragora.api.notificacao;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.boragora.api.usuario.Usuario;
import br.com.boragora.api.usuario.UsuarioRepository;

/**
 * Único caminho de envio: ignora quem não tem token, apaga o token que o Expo rejeitar e nunca
 * deixa uma falha do envio chegar a quem chamou (só registra em log).
 */
@Component
public class EnvioDePush {

    private static final Logger log = LoggerFactory.getLogger(EnvioDePush.class);

    private final PushGateway gateway;
    private final UsuarioRepository usuarios;

    public EnvioDePush(PushGateway gateway, UsuarioRepository usuarios) {
        this.gateway = gateway;
        this.usuarios = usuarios;
    }

    public void enviar(Usuario destinatario, String titulo, String corpo, TipoDePush tipo, Long turmaId) {
        String token = destinatario.getExpoPushToken();
        if (token == null) {
            return;
        }
        try {
            MensagemPush mensagem = new MensagemPush(token, titulo, corpo,
                    Map.of("tipo", tipo.name(), "turmaId", turmaId));
            if (gateway.enviar(mensagem) == ResultadoDoEnvio.TOKEN_INVALIDO) {
                usuarios.apagarToken(token);
                log.info("Token de push do usuário {} apagado: aparelho não registrado no Expo", destinatario.getId());
            }
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar push {} ao usuário {}", tipo, destinatario.getId(), e);
        }
    }
}
