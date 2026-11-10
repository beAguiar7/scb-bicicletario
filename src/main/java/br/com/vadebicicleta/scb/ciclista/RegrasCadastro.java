package br.com.vadebicicleta.scb.ciclista;

/**
 * Regras desta etapa. A senha minima de 8 caracteres foi definida pelo grupo.
 */
public final class RegrasCadastro {

	public static final int SENHA_MINIMA = 8;

	public static final int SENHA_MAXIMA = 72;

	public static final long FOTO_MAXIMA_BYTES = 5L * 1024 * 1024;

	private RegrasCadastro() {
	}

}
