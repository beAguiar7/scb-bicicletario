package br.com.vadebicicleta.scb.ciclista;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mesmas regras do cadastro, sem senha. A senha nao muda nesta etapa.
 */
public class AtualizacaoCiclistaForm {

	@NotBlank(message = "Informe o nome.")
	@Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
	private String nome;

	@NotNull(message = "Informe a nacionalidade.")
	private Nacionalidade nacionalidade;

	@NotBlank(message = "Informe o número do documento.")
	@Pattern(regexp = "^[A-Za-z0-9]{5,30}$", message = "O documento deve ter de 5 a 30 letras ou números.")
	private String numeroDocumento;

	@NotBlank(message = "Informe o e-mail.")
	@Email(message = "Informe um e-mail válido.")
	@Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
	private String email;

	public String getNome() {
		return this.nome;
	}

	public void setNome(String nome) {
		this.nome = nome == null ? null : nome.trim();
	}

	public Nacionalidade getNacionalidade() {
		return this.nacionalidade;
	}

	public void setNacionalidade(Nacionalidade nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

	public String getNumeroDocumento() {
		return this.numeroDocumento;
	}

	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento == null ? null : numeroDocumento.trim();
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
