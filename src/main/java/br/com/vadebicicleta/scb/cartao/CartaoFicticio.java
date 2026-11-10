package br.com.vadebicicleta.scb.cartao;

import java.util.List;

import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

/**
 * Cartoes de demonstracao. Nao sao numeros reais e nao existe CVV.
 * Cadastrar qualquer um dos dois da certo. A cobranca pode ser recusada depois.
 */
public enum CartaoFicticio {

	DEMO_APROVADO("DEMO-APROVADO", "**** **** **** 1111", "Demonstração Aprovado", true),
	DEMO_RECUSADO("DEMO-RECUSADO", "**** **** **** 0002", "Demonstração Recusado", false);

	private final String referencia;

	private final String mascara;

	private final String titular;

	private final boolean cobrancaAprovada;

	CartaoFicticio(String referencia, String mascara, String titular, boolean cobrancaAprovada) {
		this.referencia = referencia;
		this.mascara = mascara;
		this.titular = titular;
		this.cobrancaAprovada = cobrancaAprovada;
	}

	public String getReferencia() {
		return this.referencia;
	}

	public String getMascara() {
		return this.mascara;
	}

	public String getTitular() {
		return this.titular;
	}

	public boolean isCobrancaAprovada() {
		return this.cobrancaAprovada;
	}

	public static CartaoFicticio porReferencia(String referencia) {
		if (referencia != null) {
			String limpa = referencia.trim();
			for (CartaoFicticio cartao : values()) {
				if (cartao.referencia.equals(limpa)) {
					return cartao;
				}
			}
		}
		throw new ErroDeNegocio(400, "CARTAO_DESCONHECIDO", "Escolha um dos cartões de demonstração.", List.of());
	}

	public static List<CartaoFicticio> todos() {
		return List.of(values());
	}

}
