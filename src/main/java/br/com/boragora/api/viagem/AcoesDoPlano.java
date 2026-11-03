package br.com.boragora.api.viagem;

/**
 * O que o aluno pode fazer agora, calculado com o relógio do servidor para o app não repetir
 * a lógica de horário. {@code cancelamentoIdaSeraTardio} indica que cancelar a ida agora avisa
 * o motorista (tela B4).
 */
public record AcoesDoPlano(
        boolean podeIncluirIda,
        boolean podeCancelarIda,
        boolean cancelamentoIdaSeraTardio,
        boolean podeAlterarVolta,
        boolean podeAlterarStatusVolta) {
}
