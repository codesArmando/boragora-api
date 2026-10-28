package br.com.boragora.api.turma;

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
@Tag(name = "Turmas")
public class TurmaController {

    private final TurmaService service;

    public TurmaController(TurmaService service) {
        this.service = service;
    }

    @GetMapping("/admin/turmas")
    @Operation(summary = "Lista cadastral de todas as turmas (tela D3)")
    public List<TurmaCadastroResposta> listarCadastro() {
        return service.listarCadastro();
    }

    @PostMapping("/admin/turmas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma turma (tela D4)")
    public TurmaResposta criar(@RequestBody @Valid TurmaCriacaoRequisicao requisicao) {
        return service.criar(requisicao);
    }

    @PutMapping("/admin/turmas/{id}")
    @Operation(summary = "Edita os dados cadastrais e troca o motorista (telas D5 e D5a)")
    public TurmaResposta atualizar(@PathVariable Long id, @RequestBody @Valid TurmaEdicaoRequisicao requisicao) {
        return service.atualizar(id, requisicao);
    }

    @DeleteMapping("/admin/turmas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a turma com seus vínculos, viagens e confirmações (tela D5a)")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @GetMapping("/turmas/{id}")
    @Operation(summary = "Turma com grupo, motorista e alunos ativos, respeitando a visibilidade (telas C4, D5 e D5a)")
    public TurmaResposta detalhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.detalhar(id, usuario);
    }
}
