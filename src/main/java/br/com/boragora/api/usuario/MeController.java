package br.com.boragora.api.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.boragora.api.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/me")
public class MeController {

    private final PushTokenService pushTokens;

    public MeController(PushTokenService pushTokens) {
        this.pushTokens = pushTokens;
    }

    @GetMapping
    @Operation(summary = "Dados do usuário autenticado")
    public UsuarioResposta me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioResposta.de(usuario);
    }

    @PutMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registra o token do Expo do aparelho (depois do login)")
    public void registrarPushToken(@RequestBody @Valid PushTokenRequisicao requisicao,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        pushTokens.registrar(usuario.id(), requisicao.token());
    }

    @DeleteMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o token do Expo (ao sair da conta)")
    public void removerPushToken(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        pushTokens.remover(usuario.id());
    }
}
