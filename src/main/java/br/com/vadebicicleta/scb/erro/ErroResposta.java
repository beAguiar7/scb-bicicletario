package br.com.vadebicicleta.scb.erro;

import java.util.List;

public record ErroResposta(String codigo, String mensagem, List<ErroCampo> erros) {

	public ErroResposta {
		erros = erros == null ? List.of() : List.copyOf(erros);
	}

	public record ErroCampo(String campo, String mensagem) {
	}

}
