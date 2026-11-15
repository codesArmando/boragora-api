package br.com.boragora.api.turma;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Alunos")
public class AlunoController {

    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @GetMapping("/admin/alunos")
    @Operation(summary = "Alunos ativos; com turmaId traz a situação em relação à turma (telas D6 e D10)")
    public List<AlunoAdminResposta> listar(
            @Parameter(description = "Trecho do nome ou do e-mail") @RequestParam(required = false) String busca,
            @Parameter(description = "Turma do vínculo (tela D6); sem ela, listagem geral (tela D10)")
            @RequestParam(required = false) Long turmaId) {
        return service.listar(busca, turmaId);
    }
}
