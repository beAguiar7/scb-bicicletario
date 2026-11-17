package br.com.vadebicicleta.scb.aluguel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedidoDevolucao(
		@NotBlank(message = "Informe o RFID da bicicleta.")
		@Size(max = 40, message = "Informe o RFID da bicicleta.")
		String rfid,
		@NotBlank(message = "Informe o número da tranca.")
		@Size(max = 20, message = "Informe o número da tranca.")
		String numeroTranca) {

}
