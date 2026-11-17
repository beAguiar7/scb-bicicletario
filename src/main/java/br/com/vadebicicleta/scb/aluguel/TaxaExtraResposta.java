package br.com.vadebicicleta.scb.aluguel;

/**
 * Valor e situacao da taxa extra. Nao leva ciclista nem cartao.
 */
public record TaxaExtraResposta(String valor, String situacao, boolean simulacao) {

}
