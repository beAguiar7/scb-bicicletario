package br.com.vadebicicleta.scb.aluguel;

public record MensagemResposta(Long id, String assunto, String texto, String criadaEm) {

	static MensagemResposta de(MensagemSimulada mensagem) {
		return new MensagemResposta(mensagem.getId(), mensagem.getAssunto(), mensagem.getTexto(),
				mensagem.getCriadaEm().toString());
	}

}
