package br.com.vadebicicleta.scb.aluguel;

/**
 * Taxa extra pendente visivel para o proprio ciclista. Nao leva cartao.
 */
public record TaxaPendenteResposta(Long id, String valor, String situacao, String horario) {

}
