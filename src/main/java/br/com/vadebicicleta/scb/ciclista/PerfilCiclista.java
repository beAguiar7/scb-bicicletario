package br.com.vadebicicleta.scb.ciclista;

/**
 * Dados do proprio ciclista, para preencher a tela de alteracao.
 * Nao inclui senha, hash nem foto.
 */
public record PerfilCiclista(Long id, String nome, Nacionalidade nacionalidade, String numeroDocumento, String email) {

	public static PerfilCiclista de(Ciclista ciclista) {
		return new PerfilCiclista(ciclista.getId(), ciclista.getNome(), ciclista.getNacionalidade(),
				ciclista.getNumeroDocumento(), ciclista.getEmail());
	}

}
