package br.com.vadebicicleta.scb.erro;

import java.util.List;

/**
 * Falha esperada, com mensagem que pode ser mostrada ao ciclista.
 */
public class ErroDeNegocio extends RuntimeException {

	private final int status;

	private final String codigo;

	private final List<ErroResposta.ErroCampo> erros;

	public ErroDeNegocio(int status, String codigo, String mensagem, List<ErroResposta.ErroCampo> erros) {
		super(mensagem);
		this.status = status;
		this.codigo = codigo;
		this.erros = erros == null ? List.of() : List.copyOf(erros);
	}

	public static ErroDeNegocio campo(String campo, String mensagem) {
		return new ErroDeNegocio(400, "DADOS_INVALIDOS", mensagem,
				List.of(new ErroResposta.ErroCampo(campo, mensagem)));
	}

	public static ErroDeNegocio emailJaCadastrado() {
		String mensagem = "Este e-mail já está cadastrado.";
		return new ErroDeNegocio(409, "EMAIL_JA_CADASTRADO", mensagem,
				List.of(new ErroResposta.ErroCampo("email", mensagem)));
	}

	public static ErroDeNegocio credenciaisInvalidas() {
		return new ErroDeNegocio(401, "CREDENCIAIS_INVALIDAS", "Login ou senha inválidos", List.of());
	}

	public static ErroDeNegocio arquivoMuitoGrande() {
		String mensagem = "A foto deve ter no máximo 5 MB.";
		return new ErroDeNegocio(413, "ARQUIVO_MUITO_GRANDE", mensagem,
				List.of(new ErroResposta.ErroCampo("foto", mensagem)));
	}

	public int getStatus() {
		return this.status;
	}

	public String getCodigo() {
		return this.codigo;
	}

	public List<ErroResposta.ErroCampo> getErros() {
		return this.erros;
	}

}
