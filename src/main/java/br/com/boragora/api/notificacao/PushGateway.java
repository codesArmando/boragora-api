package br.com.boragora.api.notificacao;

/** Envio de push. Qualquer falha que não seja de token inválido é lançada como exceção. */
public interface PushGateway {

    ResultadoDoEnvio enviar(MensagemPush mensagem);
}
