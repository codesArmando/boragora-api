package br.com.boragora.api.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TratadorDeErrosTest {

    private final TratadorDeErros tratador = new TratadorDeErros();

    @Test
    void regraDeNegocioUsaCodigoEStatusDaExcecao() {
        var e = new RegraDeNegocioException("IDA_ENCERRADA", HttpStatus.UNPROCESSABLE_CONTENT, "Não dá mais.");

        ResponseEntity<ErroResposta> resposta = tratador.regraDeNegocio(e);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody()).isEqualTo(new ErroResposta("IDA_ENCERRADA", "Não dá mais.", null));
    }

    @Test
    void erroInesperadoVira500SemExporDetalhes() {
        ResponseEntity<ErroResposta> resposta = tratador.outrosErros(new IllegalStateException("detalhe interno"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().codigo()).isEqualTo("ERRO_INTERNO");
        assertThat(resposta.getBody().mensagem()).doesNotContain("detalhe interno");
    }
}
