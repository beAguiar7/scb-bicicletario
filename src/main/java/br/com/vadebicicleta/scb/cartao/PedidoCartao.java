package br.com.vadebicicleta.scb.cartao;

import jakarta.validation.constraints.NotBlank;

public record PedidoCartao(@NotBlank(message = "Escolha um cartão de demonstração.") String referencia) {

}
