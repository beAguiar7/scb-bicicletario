package br.com.vadebicicleta.scb.aluguel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PedidoAluguel(
		@NotBlank(message = "Não foi possível confirmar o aluguel.")
		@Pattern(regexp = "^[A-Za-z0-9-]{8,80}$", message = "Não foi possível confirmar o aluguel.")
		String chave,
		@NotBlank(message = "Informe o número da tranca.")
		@Size(max = 20, message = "Informe o número da tranca.")
		String numeroTranca) {

}
