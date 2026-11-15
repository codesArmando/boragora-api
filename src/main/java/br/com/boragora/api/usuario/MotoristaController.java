package br.com.boragora.api.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/motoristas")
@Tag(name = "Motoristas")
public class MotoristaController {

    private final MotoristaService service;

    public MotoristaController(MotoristaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um motorista (tela D7)")
    public MotoristaResposta cadastrar(@RequestBody @Valid MotoristaRequisicao requisicao) {
        return service.cadastrar(requisicao);
    }

    @GetMapping
    @Operation(summary = "Motoristas ativos, incluindo o chefe (tela D9 e seletores das telas D4 e D5)")
    public List<MotoristaResposta> listar() {
        return service.listar();
    }
}
