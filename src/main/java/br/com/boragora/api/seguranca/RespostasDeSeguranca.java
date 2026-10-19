package br.com.boragora.api.seguranca;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import br.com.boragora.api.compartilhado.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Respostas 401 e 403 geradas pelos filtros de segurança, no formato padrão de erro. */
@Component
class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final ErroResposta NAO_AUTENTICADO =
            ErroResposta.de("NAO_AUTENTICADO", "Sessão ausente, inválida ou expirada. Faça login novamente.");
    static final ErroResposta ACESSO_NEGADO =
            ErroResposta.de("ACESSO_NEGADO", "Você não tem permissão para acessar este recurso.");

    private final JsonMapper json;

    RespostasDeSeguranca(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException e)
            throws IOException {
        resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escrever(resposta, HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException e)
            throws IOException {
        escrever(resposta, HttpStatus.FORBIDDEN, ACESSO_NEGADO);
    }

    private void escrever(HttpServletResponse resposta, HttpStatus status, ErroResposta erro) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), erro);
    }
}
