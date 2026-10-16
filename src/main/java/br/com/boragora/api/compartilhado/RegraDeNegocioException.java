package br.com.boragora.api.compartilhado;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exceção base das regras de negócio. O {@code codigo} vai no corpo do erro
 * (ex.: {@code IDA_ENCERRADA}) e o {@code status} define a resposta HTTP.
 * {@code campos} é opcional e indica os campos da requisição envolvidos.
 */
public class RegraDeNegocioException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;
    private final Map<String, String> campos;

    public RegraDeNegocioException(String codigo, HttpStatus status, String mensagem) {
        this(codigo, status, mensagem, null);
    }

    public RegraDeNegocioException(String codigo, HttpStatus status, String mensagem, Map<String, String> campos) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
        this.campos = campos;
    }

    /** 400 {@code VALIDACAO} para regras que dependem de mais de um campo. */
    public static RegraDeNegocioException validacao(String campo, String mensagem) {
        return new RegraDeNegocioException("VALIDACAO", HttpStatus.BAD_REQUEST, mensagem, Map.of(campo, mensagem));
    }

    /** 404 {@code NAO_ENCONTRADO}. */
    public static RegraDeNegocioException naoEncontrado(String mensagem) {
        return new RegraDeNegocioException("NAO_ENCONTRADO", HttpStatus.NOT_FOUND, mensagem);
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
