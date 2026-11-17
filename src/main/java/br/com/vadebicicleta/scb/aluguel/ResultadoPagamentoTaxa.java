package br.com.vadebicicleta.scb.aluguel;

/**
 * Resultado do reprocessamento. O valor e o horario original da HU07 permanecem.
 * jaPaga indica que o simulador nao foi chamado nesta requisicao.
 */
public record ResultadoPagamentoTaxa(Long id, String valor, String situacao, String horario, String tentativa,
		String mensagem, boolean simulacao, boolean jaPaga) {

}
