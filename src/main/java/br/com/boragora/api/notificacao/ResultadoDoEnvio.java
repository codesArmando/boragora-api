package br.com.boragora.api.notificacao;

public enum ResultadoDoEnvio {
    ENVIADO,
    /** O Expo respondeu {@code DeviceNotRegistered}: o token não serve mais e deve ser apagado. */
    TOKEN_INVALIDO
}
