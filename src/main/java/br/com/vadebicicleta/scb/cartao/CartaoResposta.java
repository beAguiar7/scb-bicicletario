package br.com.vadebicicleta.scb.cartao;

public record CartaoResposta(boolean cadastrado, String referencia, String mascara, String titular,
		Boolean cobrancaSimuladaAprovada, String aviso) {

	static CartaoResposta vazio() {
		return new CartaoResposta(false, null, null, null, null,
				"Nenhum cartão de demonstração cadastrado. Nenhuma cobrança real é feita.");
	}

	static CartaoResposta de(CartaoDemonstracao cartao) {
		String aviso = cartao.isCobrancaSimuladaAprovada()
				? "Cartão de demonstração cadastrado. Na simulação, a cobrança deste cartão será aprovada. Nenhuma cobrança real é feita."
				: "Cartão de demonstração cadastrado. Na simulação, a cobrança deste cartão será recusada. Nenhuma cobrança real é feita.";
		return new CartaoResposta(true, cartao.getReferencia(), cartao.getMascara(), cartao.getTitular(),
				cartao.isCobrancaSimuladaAprovada(), aviso);
	}

}
