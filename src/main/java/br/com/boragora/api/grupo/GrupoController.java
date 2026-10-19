package br.com.boragora.api.grupo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.boragora.api.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Grupos")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @PostMapping("/admin/grupos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um grupo (tela D2)")
    public GrupoResposta criar(@RequestBody @Valid GrupoRequisicao requisicao,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.criar(requisicao, usuario);
    }

    @PutMapping("/admin/grupos/{id}")
    @Operation(summary = "Edita um grupo (tela D2 em modo edição)")
    public GrupoResposta atualizar(@PathVariable Long id, @RequestBody @Valid GrupoRequisicao requisicao,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.atualizar(id, requisicao, usuario);
    }

    @DeleteMapping("/admin/grupos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um grupo sem turmas")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @GetMapping("/grupos")
    @Operation(summary = "Grupos visíveis para quem chama (telas D1, B8a e C3a)")
    public List<GrupoResposta> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.listar(usuario);
    }

    @GetMapping("/grupos/{id}")
    @Operation(summary = "Um grupo, respeitando a visibilidade de quem chama")
    public GrupoResposta detalhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.detalhar(id, usuario);
    }
}
