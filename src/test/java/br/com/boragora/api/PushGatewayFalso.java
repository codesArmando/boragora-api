package br.com.boragora.api;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import br.com.boragora.api.notificacao.MensagemPush;
import br.com.boragora.api.notificacao.PushGateway;
import br.com.boragora.api.notificacao.ResultadoDoEnvio;

/**
 * Gateway dos testes de integração: registra cada envio em vez de chamar o Expo. Pode simular
 * falha geral ou token não registrado. Quem configura algo chama {@link #limpar()} no fim do teste.
 */
public class PushGatewayFalso implements PushGateway {

    private final List<MensagemPush> enviadas = new CopyOnWriteArrayList<>();
    private final Set<String> tokensNaoRegistrados = ConcurrentHashMap.newKeySet();
    private volatile RuntimeException falha;

    @Override
    public ResultadoDoEnvio enviar(MensagemPush mensagem) {
        enviadas.add(mensagem);
        if (falha != null) {
            throw falha;
        }
        return tokensNaoRegistrados.contains(mensagem.token()) ? ResultadoDoEnvio.TOKEN_INVALIDO
                : ResultadoDoEnvio.ENVIADO;
    }

    /** Envios para o token informado, na ordem em que aconteceram. */
    public List<MensagemPush> enviadasPara(String token) {
        return enviadas.stream().filter(mensagem -> mensagem.token().equals(token)).toList();
    }

    public void falharCom(RuntimeException falha) {
        this.falha = falha;
    }

    public void naoRegistrado(String token) {
        tokensNaoRegistrados.add(token);
    }

    public void limpar() {
        enviadas.clear();
        tokensNaoRegistrados.clear();
        falha = null;
    }
}
