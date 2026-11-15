package br.com.boragora.api.notificacao;

import java.util.Map;

/** {@code data} é o que o app usa para abrir a tela certa ao tocar na notificação ({@code tipo} e {@code turmaId}). */
public record MensagemPush(String token, String titulo, String corpo, Map<String, Object> data) {
}
