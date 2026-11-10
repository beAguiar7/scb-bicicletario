package br.com.vadebicicleta.scb.ciclista;

/**
 * Dados que podem voltar ao navegador. Senha, hash e foto ficam de fora.
 */
public record CiclistaResposta(Long id, String nome, Nacionalidade nacionalidade, String email) {

	public static CiclistaResposta de(Ciclista ciclista) {
		return new CiclistaResposta(
				ciclista.getId(),
				ciclista.getNome(),
				ciclista.getNacionalidade(),
				ciclista.getEmail());
	}

}
