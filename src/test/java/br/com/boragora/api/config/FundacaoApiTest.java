package br.com.boragora.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import br.com.boragora.api.DadosDeTeste;
import br.com.boragora.api.TesteDeIntegracao;

@TesteDeIntegracao
class FundacaoApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DadosDeTeste dados;

    @Test
    void healthRespondeOk() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rotaInexistenteResponde404NoFormatoPadrao() throws Exception {
        mvc.perform(get("/rota-que-nao-existe").header(HttpHeaders.AUTHORIZATION, dados.bearer(dados.aluno())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void swaggerUiDisponivel() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/health']").exists());
    }
}
