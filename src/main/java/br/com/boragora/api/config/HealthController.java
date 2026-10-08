package br.com.boragora.api.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
public class HealthController {

    public record HealthResposta(String status) {
    }

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(summary = "Indica se a API está no ar")
    public HealthResposta health() {
        return new HealthResposta("UP");
    }
}
