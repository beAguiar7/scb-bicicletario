package br.com.vadebicicleta.scb.aluguel;

/**
 * Confirmacao publica da devolucao. Nao leva dados do ciclista nem do cartao.
 * A taxa extra fica nula quando a devolucao ocorreu dentro do periodo incluido.
 */
public record ResultadoDevolucao(String mensagem, String rfid, String numeroTranca, String horario, boolean repetida,
		TaxaExtraResposta taxaExtra) {

}
