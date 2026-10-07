package br.com.boragora.api.compartilhado;

import org.springframework.http.HttpStatus;

/**
 * Exceção base das regras de negócio. O {@code codigo} vai no corpo do erro
 * (ex.: {@code IDA_ENCERRADA}) e o {@code status} define a resposta HTTP.
 */
public class RegraDeNegocioException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public RegraDeNegocioException(String codigo, HttpStatus status, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
