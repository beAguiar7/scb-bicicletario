package br.com.vadebicicleta.scb.aluguel;

public record ResultadoAluguel(String resultado, String mensagem, Long cobrancaId, String valor, String dataHora,
		boolean simulacao, AluguelResumo aluguel) {

}
