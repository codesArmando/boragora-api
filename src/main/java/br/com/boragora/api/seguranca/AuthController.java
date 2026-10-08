package br.com.boragora.api.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@SecurityRequirements
public class AuthController {

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria a conta de um aluno (tela A2)")
    public TokenResposta cadastrar(@RequestBody @Valid CadastroRequisicao requisicao) {
        return autenticacao.cadastrar(requisicao);
    }

    @PostMapping("/login")
    @Operation(summary = "Entra com e-mail e senha (tela A1)")
    public TokenResposta entrar(@RequestBody @Valid LoginRequisicao requisicao) {
        return autenticacao.entrar(requisicao);
    }
}
