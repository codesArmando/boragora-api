package br.com.boragora.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.boragora.api.RelogioDeTeste;
import br.com.boragora.api.TesteDeIntegracao;
import br.com.boragora.api.grupo.GrupoRepository;
import br.com.boragora.api.turma.TurmaAlunoRepository;
import br.com.boragora.api.turma.TurmaRepository;
import br.com.boragora.api.usuario.UsuarioRepository;
import br.com.boragora.api.viagem.ConfirmacaoRepository;
import br.com.boragora.api.viagem.ViagemRepository;
import com.jayway.jsonpath.JsonPath;

/** A carga só existe no perfil dev; aqui ela é montada à mão sobre o banco dos testes. */
@TesteDeIntegracao
class CargaDeDemonstracaoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 5);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RelogioDeTeste relogio;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private GrupoRepository grupos;
    @Autowired
    private TurmaRepository turmas;
    @Autowired
    private TurmaAlunoRepository vinculos;
    @Autowired
    private ViagemRepository viagens;
    @Autowired
    private ConfirmacaoRepository confirmacoes;
    @Autowired
    private PasswordEncoder senhas;

    @AfterEach
    void voltarAoRelogioReal() {
        relogio.voltarAoReal();
    }

    @Test
    void cargaReproduzOCenarioDasTelasERodaUmaVezSo() throws Exception {
        relogio.fixar(DIA.atTime(19, 0));
        CargaDeDemonstracao carga = new CargaDeDemonstracao(usuarios, grupos, turmas, vinculos, viagens,
                confirmacoes, senhas, relogio);
        carga.run(null);
        long usuariosDepois = usuarios.count();
        carga.run(null);
        assertThat(usuarios.count()).isEqualTo(usuariosDepois);

        String vanderlei = entrar("vanderlei@boragora.local");
        String turmasDoVanderlei = mvc.perform(get("/motorista/turmas").header(HttpHeaders.AUTHORIZATION, vanderlei))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(turmasDoVanderlei, "$[*].grupo.nome")).containsExactly("Manhã", "Noite");
        assertThat(JsonPath.<List<String>>read(turmasDoVanderlei, "$[*].nome")).containsExactly("Van 2", "Van 2");
        Integer noiteVan2 = JsonPath.read(turmasDoVanderlei, "$[1].id");

        mvc.perform(get("/turmas/{id}/hoje", noiteVan2).header(HttpHeaders.AUTHORIZATION, vanderlei))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contadores.vaoNaIda").value(5))
                .andExpect(jsonPath("$.contadores.voltam").value(7))
                .andExpect(jsonPath("$.contadores.liberados").value(2))
                .andExpect(jsonPath("$.contadores.canceladosAposSaida").value(1))
                .andExpect(jsonPath("$.listaIda[0].nome").value("Camila Rocha"))
                .andExpect(jsonPath("$.listaIda[0].situacao").value("CANCELADO_APOS_SAIDA"));
        mvc.perform(get("/turmas/{id}", noiteVan2).header(HttpHeaders.AUTHORIZATION, vanderlei))
                .andExpect(jsonPath("$.alunos.length()").value(8))
                .andExpect(jsonPath("$.alunos[0].nome").value("Beatriz Souza"));

        // João Gabriel em Manhã · Van 1 e Noite · Van 2.
        mvc.perform(get("/aluno/turmas").header(HttpHeaders.AUTHORIZATION, entrar("joao@boragora.local")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].turma.nome").value("Van 1"))
                .andExpect(jsonPath("$[0].turma.grupo.nome").value("Manhã"))
                .andExpect(jsonPath("$[1].turma.nome").value("Van 2"))
                .andExpect(jsonPath("$[1].incluiIda").value(true));

        // Alunos sem vínculo para as telas D6 e D10.
        String admin = entrar("admin@teste.com", "senha-teste");
        for (String nome : List.of("Ana Beatriz Moura", "Diego Farias", "Vitor Sampaio")) {
            mvc.perform(get("/admin/alunos").param("busca", nome).header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(jsonPath("$[0].nome").value(nome))
                    .andExpect(jsonPath("$[0].turmas.length()").value(0));
        }
    }

    private String entrar(String email) throws Exception {
        return entrar(email, CargaDeDemonstracao.SENHA);
    }

    private String entrar(String email, String senha) throws Exception {
        String json = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }
}
