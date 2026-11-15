package br.com.boragora.api.viagem;

public enum SituacaoIda {
    CONFIRMADO,
    /** Cancelou depois da saída da ida: aparece destacado na lista, fora da contagem (RN05). */
    CANCELADO_APOS_SAIDA
}
