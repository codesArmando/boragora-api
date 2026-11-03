package br.com.boragora.api.viagem;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.boragora.api.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Listagens do dia")
public class ListagemDoDiaController {

    private final ListagemDoDiaService service;

    public ListagemDoDiaController(ListagemDoDiaService service) {
        this.service = service;
    }

    @GetMapping("/motorista/turmas")
    @Operation(summary = "Turmas que o motorista conduz, com os contadores de hoje (telas C0, C0b e C6)")
    public List<TurmaResumoHoje> listarDoMotorista(@AuthenticationPrincipal UsuarioAutenticado motorista) {
        return service.listarDoMotorista(motorista);
    }

    @GetMapping("/turmas/{id}/hoje")
    @Operation(summary = "Contadores e listas de hoje de uma turma, respeitando a visibilidade (telas C1, C2 e C5)")
    public TurmaHojeResposta hojeDaTurma(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.hojeDaTurma(id, usuario);
    }

    @GetMapping("/grupos/{id}/hoje")
    @Operation(summary = "Dia de todas as turmas do grupo, respeitando a visibilidade (telas B8, B8b e C3)")
    public GrupoHojeResposta hojeDoGrupo(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.hojeDoGrupo(id, usuario);
    }
}
