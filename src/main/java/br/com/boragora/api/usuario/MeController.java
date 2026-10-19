package br.com.boragora.api.usuario;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.boragora.api.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/me")
public class MeController {

    @GetMapping
    @Operation(summary = "Dados do usuário autenticado")
    public UsuarioResposta me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioResposta.de(usuario);
    }
}
