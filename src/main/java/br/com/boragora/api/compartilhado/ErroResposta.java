package br.com.boragora.api.compartilhado;

import java.util.Map;

/** Formato padrão de erro da API (plano, seção 2.3). {@code campos} só vem preenchido em erros de validação. */
public record ErroResposta(String codigo, String mensagem, Map<String, String> campos) {

    public static ErroResposta de(String codigo, String mensagem) {
        return new ErroResposta(codigo, mensagem, null);
    }
}
