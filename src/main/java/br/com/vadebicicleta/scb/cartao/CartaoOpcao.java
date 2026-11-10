package br.com.vadebicicleta.scb.cartao;

public record CartaoOpcao(String referencia, String mascara, String titular, boolean cobrancaSimuladaAprovada,
		String descricao) {

	static CartaoOpcao de(CartaoFicticio cartao) {
		String descricao = cartao.isCobrancaAprovada()
				? "A cobrança simulada será aprovada."
				: "A cobrança simulada será recusada.";
		return new CartaoOpcao(cartao.getReferencia(), cartao.getMascara(), cartao.getTitular(),
				cartao.isCobrancaAprovada(), descricao);
	}

}
