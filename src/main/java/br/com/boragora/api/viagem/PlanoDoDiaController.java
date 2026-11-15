package br.com.boragora.api.viagem;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.boragora.api.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/aluno/turmas")
@Tag(name = "Plano do dia")
public class PlanoDoDiaController {

    private final PlanoDoDiaService service;

    public PlanoDoDiaController(PlanoDoDiaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Turmas do aluno com o resumo do plano de hoje (tela inicial do aluno, A3 e B9)")
    public List<TurmaDoAlunoResposta> listarTurmas(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return service.listarTurmas(aluno);
    }

    @GetMapping("/{turmaId}/plano")
    @Operation(summary = "Plano de hoje na turma, com as ações permitidas agora (telas B1 a B7)")
    public PlanoResposta plano(@PathVariable Long turmaId, @AuthenticationPrincipal UsuarioAutenticado aluno) {
        return service.planoDeHoje(aluno, turmaId);
    }

    @PutMapping("/{turmaId}/plano/ida")
    @Operation(summary = "Marca ou desmarca a ida de hoje")
    public PlanoResposta alterarIda(@PathVariable Long turmaId, @RequestBody @Valid MarcacaoRequisicao requisicao,
            @AuthenticationPrincipal UsuarioAutenticado aluno) {
        return service.alterarIda(aluno, turmaId, requisicao.incluir());
    }

    @PutMapping("/{turmaId}/plano/volta")
    @Operation(summary = "Marca ou desmarca a volta de hoje")
    public PlanoResposta alterarVolta(@PathVariable Long turmaId, @RequestBody @Valid MarcacaoRequisicao requisicao,
            @AuthenticationPrincipal UsuarioAutenticado aluno) {
        return service.alterarVolta(aluno, turmaId, requisicao.incluir());
    }

    @PutMapping("/{turmaId}/plano/status-volta")
    @Operation(summary = "Informa se está em aula ou liberado para a volta")
    public PlanoResposta alterarStatusVolta(@PathVariable Long turmaId,
            @RequestBody @Valid StatusVoltaRequisicao requisicao, @AuthenticationPrincipal UsuarioAutenticado aluno) {
        return service.alterarStatusVolta(aluno, turmaId, requisicao.status());
    }
}
