package br.com.boragora.api.compartilhado;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte as exceções no formato padrão de erro da API. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> regraDeNegocio(RegraDeNegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErroResposta(e.getCodigo(), e.getMessage(), e.getCampos()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ErroResposta("VALIDACAO", "Há campos inválidos na requisição.", campos));
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErroResposta> requisicaoMalFormada(Exception e) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.de("VALIDACAO", "A requisição está em formato inválido."));
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<ErroResposta> naoEncontrado(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.de("NAO_ENCONTRADO", "Recurso não encontrado."));
    }

    /** Negações vindas de dentro dos controllers e services (os filtros de segurança têm resposta própria). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> acessoNegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResposta.de("ACESSO_NEGADO", "Você não tem permissão para acessar este recurso."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> naoAutenticado(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResposta.de("NAO_AUTENTICADO", "Sessão ausente, inválida ou expirada. Faça login novamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> outrosErros(Exception e) {
        // Erros do próprio Spring MVC (405, 415, 406...) já trazem o status HTTP adequado.
        if (e instanceof ErrorResponse erroHttp && !erroHttp.getStatusCode().is5xxServerError()) {
            return ResponseEntity.status(erroHttp.getStatusCode())
                    .body(ErroResposta.de("REQUISICAO_INVALIDA", "Requisição não suportada por esta rota."));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResposta.de("ERRO_INTERNO", "Erro inesperado. Tente novamente."));
    }
}
