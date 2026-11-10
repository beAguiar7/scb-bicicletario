package br.com.vadebicicleta.scb.aluguel;

public record AluguelResumo(Long id, String rfid, String totem, String numeroTranca, String inicio, String valor,
		int duracaoHoras) {

}
